package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.GuildData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisGuildRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), GuildRepository {

	private val guildKey = key("guild")
	private val guildOps = RedisHashOps(guildKey, factory, Long::class.java, GuildData::class.java)

	private val shardIndex = twoWayIndex("$guildKey:shard-index", factory)

	override fun save(guild: GuildData, shardId: Int): Mono<Void> {
		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, listOf(guild.id().asLong()))
			val save = guildOps.put(guild.id().asLong(), guild)

			Mono.`when`(addToShardIndex, save)
		}
	}

	override fun delete(guildId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromShardIndex = shardIndex.removeElements(guildId)
			val remove = guildOps.remove(guildId)

			removeFromShardIndex.then(remove)
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap {
					shardIndex.deleteByGroupId(shardId)
						.then(guildOps.remove(*it.toTypedArray()))
				}
		}
	}

	override fun countGuilds(): Mono<Long> {
		return Mono.defer {
			guildOps.size()
		}
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.defer {
			guildOps.get(guildId)
		}
	}

	override fun getGuilds(): Flux<GuildData> {
		return Flux.defer {
			guildOps.values()
		}
	}
}
