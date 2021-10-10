package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.GuildData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisGuildRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), GuildRepository {

	private val hash = key("guild")
	private val hashOps = factory.createRedisHashOperations<String, Long, GuildData>()

	override fun save(guild: GuildData, shardId: Int): Mono<Void> {
		return Mono.defer {
			hashOps.put(hash, guild.id().asLong(), guild).then()
		}
	}

	override fun delete(guildId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, guildId)
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Int> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countGuilds(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.defer {
			hashOps.get(hash, guildId)
		}
	}

	override fun getGuilds(): Flux<GuildData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}
}
