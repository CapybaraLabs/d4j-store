package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.PresenceData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisPresenceRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), PresenceRepository {

	private val hash = hash("presence")
	private val hashOps = factory.createRedisHashOperations<String, String, PresenceData>()

	private fun presenceKey(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(guildId: Long, presence: PresenceData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(presence), shardIndex)
	}

	override fun saveAll(guildId: Long, presences: List<PresenceData>, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			hashOps.putAll(hash, presences.associateBy { presenceKey(guildId, it.user().id().asLong()) }).then()
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, presenceKey(guildId, userId))
				.map { toIntExact(it) }
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countPresences(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		TODO("Not yet implemented")
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.defer {
			hashOps.get(hash, presenceKey(guildId, userId))
		}
	}
}
