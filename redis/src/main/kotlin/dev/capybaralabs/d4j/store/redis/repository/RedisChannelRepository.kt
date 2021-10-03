package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.ChannelData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

internal class RedisChannelRepository(factory: RedisFactory) : ChannelRepository {

	private val channelHash = "channel"

	private val hashOps = factory.createRedisHashOperations<String, Long, ChannelData>()

	override fun save(channel: ChannelData, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			hashOps.put(channelHash, channel.id().asLong(), channel).then()
		}
	}

	override fun saveAll(channels: List<ChannelData>, shardIndex: Int): Flux<Int> {
		return Flux.defer {
			hashOps.putAll(channelHash, channels.associateBy { it.id().asLong() })
				.map { if (it) 1 else 0 } // TODO rethink the signature of the method, it doesnt really make sense here
				.toFlux()
		}
	}

	override fun delete(channelId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(channelHash, channelId)
				.map { toIntExact(it) }
		}
	}

	override fun deleteByIds(channelIds: List<Long>): Mono<Int> {
		return Mono.defer {
			hashOps.remove(channelHash, *channelIds.toTypedArray())
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			hashOps.size(channelHash)
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		// Ask the guild about it, lol
		TODO("Not yet implemented")
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			hashOps.get(channelHash, channelId)
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			hashOps.values(channelHash)
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		// look up ids from guild repo
		TODO("Not yet implemented")
	}
}
