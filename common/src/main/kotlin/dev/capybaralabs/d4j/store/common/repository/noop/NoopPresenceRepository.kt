package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopPresenceRepository : PresenceRepository {
	override fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(presencesByGuild: Map<Long, List<PresenceData>>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.just(0)
	}

	override fun countPresences(): Mono<Long> {
		return Mono.just(0)
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.empty()
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return Flux.empty()
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.empty()
	}
}
