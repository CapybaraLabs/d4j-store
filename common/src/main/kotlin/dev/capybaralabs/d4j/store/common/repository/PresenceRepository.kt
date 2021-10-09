package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PresenceRepository {
	fun save(guildId: Long, presence: PresenceData, shardIndex: Int): Mono<Void>

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	fun saveAll(guildId: Long, presences: List<PresenceData>, shardIndex: Int): Mono<Void>

	fun deleteById(guildId: Long, userId: Long): Mono<Int>
	fun deleteByGuildId(guildId: Long): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>

	fun countPresences(): Mono<Long>
	fun countPresencesInGuild(guildId: Long): Mono<Long>

	fun getPresences(): Flux<PresenceData>
	fun getPresencesInGuild(guildId: Long): Flux<PresenceData>
	fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData>
}
