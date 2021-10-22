package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.EmojiData
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisEmojiRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), EmojiRepository {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val emojiKey = key("emoji")
	private val emojiOps = RedisHashOps(emojiKey, factory, Long::class.java, EmojiData::class.java)

	private val shardIndex = twoWayIndex("$emojiKey:shard-index", factory)
	private val guildIndex = oneWayIndex("$emojiKey:guild-index", factory)
	private val gShardIndex = twoWayIndex("$emojiKey:guild-shard-index", factory)

	override fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void> {
		return saveAll(mapOf(Pair(guildId, listOf(emoji))), shardId)
	}

	override fun saveAll(emojisByGuild: Map<Long, List<EmojiData>>, shardId: Int): Mono<Void> {
		val filtered = emojisByGuild.entries
			.map { Pair(it.key, it.value.filter { emojiData -> emojiData.id().isPresent }) }
			.filter { it.second.isNotEmpty() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		val ids = filtered.flatMap { it.second }.map { it.id().orElseThrow().asLong() }
		val guilIds = filtered.map { it.first }
		val emojiMap = filtered.flatMap { it.second }.associateBy { it.id().orElseThrow().asLong() }

		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, ids)
			val addToGuildIndex = Flux.fromIterable(
				filtered
					.map { guildIndex.addElements(it.first, *it.second.map { emoji -> emoji.id().orElseThrow().asLong() }.toTypedArray()) }
			).flatMap { it }
			val addToGuildShardIndex = gShardIndex.addElements(shardId, guilIds)

			val save = emojiOps.putAll(emojiMap)

			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, save)
		}
	}

	override fun deleteByIds(emojiIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromShardIndex = shardIndex.removeElements(*emojiIds.toTypedArray())
			val deleteGuildIndexEntry = guildIndex.removeElements(guildId, *emojiIds.toTypedArray())

			val remove = emojiOps.remove(*emojiIds.toTypedArray())

			Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { emojiIdsInGuild ->
					val removeFromShardIndex = shardIndex.removeElements(*emojiIdsInGuild.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = emojiOps.remove(*emojiIdsInGuild.toTypedArray())

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

			getAllIds.flatMap { emojiOps.remove(*it.toTypedArray()) }
		}
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.defer {
			emojiOps.size()
		}
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			emojiOps.values()
		}
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { emojiOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.defer {
			emojiOps.get(emojiId)
		}
	}
}
