package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisPresenceRepository : PresenceRepository {
	override fun save(guildId: Long, presence: PresenceData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(guildId: Long, presences: List<PresenceData>, shardIndex: Int): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countPresences(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getPresences(): Flux<PresenceData> {
		TODO("Not yet implemented")
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		TODO("Not yet implemented")
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		TODO("Not yet implemented")
	}
}
