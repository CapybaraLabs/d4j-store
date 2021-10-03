package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisGuildRepository : GuildRepository {
	override fun save(guild: GuildData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun delete(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countGuilds(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		TODO("Not yet implemented")
	}

	override fun getGuilds(): Flux<GuildData> {
		TODO("Not yet implemented")
	}
}
