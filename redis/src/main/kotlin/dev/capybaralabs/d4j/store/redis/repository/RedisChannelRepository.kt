package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.ChannelData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisChannelRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix),
	ChannelRepository {

	private val hash = hash("channel")
	private val hashOps = factory.createRedisHashOperations<String, Long, ChannelData>()

	override fun save(channel: ChannelData, shardIndex: Int): Mono<Void> {
		return saveAll(listOf(channel), shardIndex)
	}

	override fun saveAll(channels: List<ChannelData>, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			hashOps.putAll(hash, channels.associateBy { it.id().asLong() }).then()
		}
	}

	override fun delete(channelId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, channelId)
				.map { toIntExact(it) }
		}
	}

	override fun deleteByIds(channelIds: List<Long>): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, *channelIds.toTypedArray())
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		// Ask the guild about it
		TODO("Not yet implemented")
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			hashOps.get(hash, channelId)
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		// look up ids from guild repo
		TODO("Not yet implemented")
	}
}
