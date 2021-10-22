package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopGuildRepository : GuildRepository {
	override fun save(guild: GuildData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(guilds: List<GuildData>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun delete(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countGuilds(): Mono<Long> {
		return Mono.empty()
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.empty()
	}

	override fun getGuilds(): Flux<GuildData> {
		return Flux.empty()
	}
}
