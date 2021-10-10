package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.ChannelData
import java.lang.StrictMath.toIntExact
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ZSetOperations.TypedTuple
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisChannelRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix),
	ChannelRepository {

	private val channelKey = key("channel")
	private val channelOps = factory.createRedisHashOperations<String, Long, ChannelData>()

	private val shardIndexKey = key("channel:shard-index")
	private val shardOps = factory.createRedisSortedSetOperations<String, Long>()

	private val guildIndexPrefix = key("channel:guild-index")
	private val guildOps = factory.createRedisSetOperations<String, Long>()

	private fun guildIndexKey(guildId: Long): String {
		return "$guildIndexPrefix:$guildId"
	}

	override fun save(channel: ChannelData, shardId: Int): Mono<Void> {
		return saveAll(listOf(channel), shardId)
	}

	override fun saveAll(channels: List<ChannelData>, shardId: Int): Mono<Void> {
		// TODO LUA script for atomicity
		return Mono.defer {
			val shardTuples = channels.map { TypedTuple.of(it.id().asLong(), shardId.toDouble()) }
			val addToShardIndex = shardOps.addAll(shardIndexKey, shardTuples)

			val addToGuildIndex = Flux.fromIterable(
				channels
					.filter { it.guildId().isPresent() }
					.groupBy { it.guildId().get().asLong() }
					.map {
						guildOps.add(
							guildIndexKey(it.key),
							*it.value.map { ch -> ch.id().asLong() }.toTypedArray()
						)
					}
			).flatMap { it }

			val saveChannels = channelOps.putAll(channelKey, channels.associateBy { it.id().asLong() })


			Mono.`when`(addToShardIndex, addToGuildIndex, saveChannels)
		}
	}

	override fun delete(channelId: Long): Mono<Int> {
		return deleteByIds(listOf(channelId))
	}

	override fun deleteByIds(channelIds: List<Long>): Mono<Int> {
		return Mono.defer {
			channelOps.remove(channelKey, *channelIds.toTypedArray())
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Int> {
		return Mono.defer {
			val shardRange = Range.just(shardId.toDouble())
			shardOps.rangeByScore(shardIndexKey, shardRange)
				.collectList()
				.flatMap { ids ->
					shardOps.removeRangeByScore(shardIndexKey, shardRange)
						.flatMap { deleteByIds(ids) } // TODO LUA script for atomicity
				}
		}
	}

	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			channelOps.size(channelKey)
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildOps.size(guildIndexKey(guildId))
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
			guildOps.members(guildIndexKey(guildId))
				.collectList()
				.flatMap { channelOps.multiGet(channelKey, it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}
}
