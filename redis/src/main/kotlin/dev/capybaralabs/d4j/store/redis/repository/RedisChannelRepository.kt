package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.ChannelData
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisChannelRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), ChannelRepository {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val channelKey = key("channel")
	private val channelOps = factory.createRedisHashOperations<String, Long, ChannelData>()

	private val shardIndex = TwoWayIndex("$channelKey:shard-index", factory)
	private val guildIndex = OneWayIndex("$channelKey:guild-index", factory)
	private val gShardIndex = TwoWayIndex("$channelKey:guild-shard-index", factory)

	override fun save(channel: ChannelData, shardId: Int): Mono<Void> {
		return saveAll(listOf(channel), shardId)
	}

	override fun saveAll(channels: List<ChannelData>, shardId: Int): Mono<Void> {
		if (channels.isEmpty()) {
			return Mono.empty()
		}
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, channels.map { it.id().asLong() })

			val addToGuildIndex = Flux.fromIterable(
				channels
					.filter { it.guildId().isPresent() }
					.groupBy { it.guildId().get().asLong() }
					.map { guildIndex.addElements(it.key, it.value.map { ch -> ch.id().asLong() }) }
			).flatMap { it }

			val guilIds = channels.filter { it.guildId().isPresent() }.map { it.guildId().get().asLong() }
			val addToGuildShardIndex = gShardIndex.addElements(shardId, guilIds)

			val saveChannels = channelOps.putAll(channelKey, channels.associateBy { it.id().asLong() })


			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, saveChannels)
		}
	}


	override fun delete(channelId: Long, guildId: Long?): Mono<Long> {
		return Mono.defer {
			val removeChannel = channelOps.remove(channelKey, channelId)
			val removeFromShardIndex = shardIndex.removeElements(listOf(channelId))
			val removeFromGuildIndex = if (guildId != null) guildIndex.removeElements(guildId, listOf(channelId)) else Mono.empty()

			Mono.`when`(removeFromShardIndex, removeFromGuildIndex)
				.then(removeChannel)
		}
	}

	override fun deleteByGuildId(channelIds: List<Long>, guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { channelIdsInGuild ->
					if (channelIds != channelIdsInGuild) {
						log.warn("Guild index deviates from ids parameter: {} vs {}", channelIdsInGuild, channelIds)
					}
					val allChannelIds = channelIds + channelIdsInGuild

					val removeChannels = channelOps.remove(channelKey, *allChannelIds.toTypedArray())

					val removeFromShardIndex = shardIndex.removeElements(allChannelIds)
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(listOf(guildId))

					Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(removeChannels)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val getChannelIds: Mono<Set<Long>> = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
			val getGuildIds: Mono<Set<Long>> = gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { gShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }

			val getAllChannelIds = getGuildIds.flatMap { guildIds ->
				// Technically we don't need to fetch the channel ids of the groups here, we can rely on the shard index only.
				guildIndex.getElementsInGroups(guildIds).collectSet()
					.flatMap { guildIndex.deleteGroups(it).then(Mono.just(it)) }
					.flatMap { idsInGuilds -> getChannelIds.map { channelIds -> idsInGuilds + channelIds } }
			}

			getAllChannelIds.flatMap { channelOps.remove(channelKey, *it.toTypedArray()) }
		}
	}


	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			channelOps.size(channelKey)
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}


	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			channelOps.get(channelKey, channelId)
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			channelOps.values(channelKey)
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { channelOps.multiGet(channelKey, it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}
}
