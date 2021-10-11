package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PresenceRepository {
	fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void>

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	fun saveAll(guildId: Long, presences: List<PresenceData>, shardId: Int): Mono<Void>

	fun deleteById(guildId: Long, userId: Long): Mono<Long>
	fun deleteByGuildId(guildId: Long): Mono<Long>
	fun deleteByShardId(shardId: Int): Mono<Long>

	fun countPresences(): Mono<Long>
	fun countPresencesInGuild(guildId: Long): Mono<Long>

	fun getPresences(): Flux<PresenceData>
	fun getPresencesInGuild(guildId: Long): Flux<PresenceData>
	fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData>
}
