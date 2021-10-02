package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface GuildRepository {
	fun save(guild: GuildData, shardIndex: Int): Mono<Void>
	fun delete(guildId: Long): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>
	fun countGuilds(): Mono<Long>
	fun getGuildById(guildId: Long): Mono<GuildData>
	fun getGuilds(): Flux<GuildData>
}
