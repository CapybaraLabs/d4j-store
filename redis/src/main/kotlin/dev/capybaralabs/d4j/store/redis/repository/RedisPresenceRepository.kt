package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisPresenceRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), PresenceRepository {

	private val presenceKey = key("presence")
	private val presenceOps = factory.createRedisHashOperations<String, String, PresenceData>()

	private fun presenceKey(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(presence), shardId)
	}

	override fun saveAll(guildId: Long, presences: List<PresenceData>, shardId: Int): Mono<Void> {
		if (presences.isEmpty()) {
			return Mono.empty()
		}
		return Mono.defer {
			presenceOps.putAll(presenceKey, presences.associateBy { presenceKey(guildId, it.user().id().asLong()) }).then()
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			presenceOps.remove(presenceKey, presenceKey(guildId, userId))
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countPresences(): Mono<Long> {
		return Mono.defer {
			presenceOps.size(presenceKey)
		}
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.defer {
			presenceOps.values(presenceKey)
		}
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		TODO("Not yet implemented")
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.defer {
			presenceOps.get(presenceKey, presenceKey(guildId, userId))
		}
	}
}
