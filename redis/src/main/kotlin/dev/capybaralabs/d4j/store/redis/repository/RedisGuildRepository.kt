package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisGuildRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), GuildRepository {

	private val guildKey = key("guild")
	private val guildOps = factory.createRedisHashOperations<String, Long, GuildData>()

	override fun save(guild: GuildData, shardId: Int): Mono<Void> {
		return Mono.defer {
			guildOps.put(guildKey, guild.id().asLong(), guild).then()
		}
	}

	override fun delete(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildOps.remove(guildKey, guildId)
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countGuilds(): Mono<Long> {
		return Mono.defer {
			guildOps.size(guildKey)
		}
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.defer {
			guildOps.get(guildKey, guildId)
		}
	}

	override fun getGuilds(): Flux<GuildData> {
		return Flux.defer {
			guildOps.values(guildKey)
		}
	}
}
