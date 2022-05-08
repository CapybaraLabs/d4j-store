package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.StickerRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.StickerData
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisStickerRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), StickerRepository {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val stickerKey = key("sticker")
	private val stickerOps = RedisHashOps(stickerKey, factory, Long::class.java, StickerData::class.java)

	private val shardIndex = twoWayIndex("$stickerKey:shard-index", factory)
	private val guildIndex = oneWayIndex("$stickerKey:guild-index", factory)
	private val gShardIndex = twoWayIndex("$stickerKey:guild-shard-index", factory)

	override fun save(guildId: Long, sticker: StickerData, shardId: Int): Mono<Void> {
		val data = StickerData.builder()
			.from(sticker)
			.guildId(guildId)
			.build()

		return saveAll(listOf(data), shardId)
	}

	override fun saveAll(stickers: List<StickerData>, shardId: Int): Mono<Void> {
		val filtered = stickers.filter { it.guildId().isPresent() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, filtered.map { it.id().asLong() })

			val addToGuildIndex = Flux.fromIterable(
				filtered
					.groupBy { it.guildId().get().asLong() }
					.map { guildIndex.addElements(it.key, *it.value.map { ch -> ch.id().asLong() }.toTypedArray()) }
			).flatMap { it }

			val guildIds = filtered.map { it.guildId().get().asLong() }
			val addToGuildShardIndex = gShardIndex.addElements(shardId, guildIds)

			val saveStickers = stickerOps.putAll(filtered.associateBy { it.id().asLong() })


			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, saveStickers)
		}
	}

	override fun deleteByIds(stickerIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromShardIndex = shardIndex.removeElements(*stickerIds.toTypedArray())
			val deleteGuildIndexEntry = guildIndex.removeElements(guildId, *stickerIds.toTypedArray())

			val remove = stickerOps.remove(*stickerIds.toTypedArray())

			Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { stickerIdsInGuild ->
					val removeFromShardIndex = shardIndex.removeElements(*stickerIdsInGuild.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = stickerOps.remove(*stickerIdsInGuild.toTypedArray())

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

			getAllIds.flatMap { stickerOps.remove(*it.toTypedArray()) }
		}
	}

	override fun countStickers(): Mono<Long> {
		return Mono.defer {
			stickerOps.size()
		}
	}

	override fun countStickersInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getStickers(): Flux<StickerData> {
		return Flux.defer {
			stickerOps.values()
		}
	}

	override fun getStickersInGuild(guildId: Long): Flux<StickerData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { stickerOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getStickerById(guildId: Long, stickerId: Long): Mono<StickerData> {
		return Mono.defer {
			stickerOps.get(stickerId)
		}
	}
}
