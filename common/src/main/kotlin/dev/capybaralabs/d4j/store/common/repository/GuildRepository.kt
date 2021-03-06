package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface GuildRepository {
	fun save(guild: GuildData, shardId: Int): Mono<Void>
	fun saveAll(guilds: List<GuildData>, shardId: Int): Mono<Void>

	fun delete(guildId: Long): Mono<Long>
	fun deleteByShardId(shardId: Int): Mono<Long>

	fun countGuilds(): Mono<Long>

	fun getGuildById(guildId: Long): Mono<GuildData>
	fun getGuilds(): Flux<GuildData>
}
