package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.EmojiData
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisEmojiRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), EmojiRepository {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val emojiKey = key("emoji")
	private val emojiOps = factory.createRedisHashOperations<String, Long, EmojiData>()

	private val shardIndex = TwoWayIndex("$emojiKey:shard-index", factory)
	private val guildIndex = OneWayIndex("$emojiKey:guild-index", factory)
	private val gShardIndex = TwoWayIndex("$emojiKey:guild-shard-index", factory)

	override fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(emoji), shardId)
	}

	override fun saveAll(guildId: Long, emojis: List<EmojiData>, shardId: Int): Mono<Void> {
		val guildEmojis = emojis.filter { it.id().isPresent }
		if (guildEmojis.isEmpty()) {
			return Mono.empty()
		}
		val ids = guildEmojis.map { it.id().orElseThrow().asLong() }

		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, ids)
			val addToGuildIndex = guildIndex.addElements(guildId, ids)
			val addToGuildShardIndex = gShardIndex.addElements(shardId, listOf(guildId))

			val save = emojiOps.putAll(emojiKey, guildEmojis.associateBy { it.id().orElseThrow().asLong() }).then()

			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, save)
		}
	}

	override fun deleteByGuildId(emojiIds: List<Long>, guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { idsInGuild ->
					if (emojiIds != idsInGuild) {
						log.warn("Guild index deviates from ids parameter: {} vs {}", idsInGuild, emojiIds)
					}
					val allIds = emojiIds + idsInGuild

					val removeFromShardIndex = shardIndex.removeElements(allIds)
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(listOf(guildId))

					val remove = emojiOps.remove(emojiKey, *allIds.toTypedArray())

					Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val getIds: Mono<Set<Long>> = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
			val getGuildIds: Mono<Set<Long>> = gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { gShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }

			val getAllIds = getGuildIds.flatMap { guildIds ->
				// Technically we don't need to fetch the ids from the groups here, we can rely on the shard index only.
				guildIndex.getElementsInGroups(guildIds).collectSet()
					.flatMap { guildIndex.deleteGroups(guildIds).then(Mono.just(it)) }
					.flatMap { idsInGuilds -> getIds.map { ids -> idsInGuilds + ids } }
			}

			getAllIds.flatMap { emojiOps.remove(emojiKey, *it.toTypedArray()) }
		}
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.defer {
			emojiOps.size(emojiKey)
		}
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			emojiOps.values(emojiKey)
		}
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { emojiOps.multiGet(emojiKey, it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.defer {
			emojiOps.get(emojiKey, emojiId)
		}
	}
}
