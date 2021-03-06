package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisPresenceRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), PresenceRepository {

	private val presenceKey = key("presence")
	private val presenceOps = RedisHashOps(presenceKey, factory, String::class.java, PresenceData::class.java)

	// a user can have "multiple" presences (due to sharding), so our indices have to handle that
	// kinda dumb to have the guildId as part of the index values here BUT we need it because it is not unique
	// we _could_ think about optimizing this
	private val guildIndex = oneWayIndex("$presenceKey:guild-index", factory, String::class.java)
	private val gShardIndex = twoWayIndex("$presenceKey:guild-shard-index", factory)

	private fun presenceId(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void> {
		return saveAll(mapOf(Pair(guildId, listOf(presence))), shardId)
	}

	override fun saveAll(presencesByGuild: Map<Long, List<PresenceData>>, shardId: Int): Mono<Void> {
		val filtered = presencesByGuild.filter { it.value.isNotEmpty() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		val presenceMap = filtered
			.flatMap { entry ->
				entry.value.associateBy { presenceId(entry.key, it.user().id().asLong()) }.entries.map { it.toPair() }
			}.toMap()
		return Mono.defer {
			val addToGuildIndex = Flux.fromIterable(
				filtered.map {
					guildIndex.addElements(
						it.key, *it.value.map { presence -> presenceId(it.key, presence.user().id().asLong()) }.toTypedArray()
					)
				}
			).flatMap { it }
			val addGuildToShardIndex = gShardIndex.addElements(shardId, filtered.keys)

			val save = presenceOps.putAll(presenceMap)

			Mono.`when`(addToGuildIndex, addGuildToShardIndex, save)
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromGuildIndex = guildIndex.removeElements(guildId, presenceId(guildId, userId))
			val remove = presenceOps.remove(presenceId(guildId, userId))

			Mono.`when`(removeFromGuildIndex)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { presenceIds ->
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = presenceOps.remove(*presenceIds.toTypedArray())

					Mono.`when`(deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { guildIds ->
					val fetchPresenceIds = guildIndex.getElementsInGroups(guildIds).collectList()

					val removeGuildsFromShardIndex = gShardIndex.deleteByGroupId(shardId)
					val deleteGuildsFromGuildIndex = guildIndex.deleteGroups(guildIds)

					fetchPresenceIds.flatMap { presenceIds ->
						Mono.`when`(removeGuildsFromShardIndex, deleteGuildsFromGuildIndex).then(Mono.just(presenceIds))
					}
				}
				.flatMap { presenceIds -> presenceOps.remove(*presenceIds.toTypedArray()) }
		}

	}

	override fun countPresences(): Mono<Long> {
		return Mono.defer {
			presenceOps.size()
		}
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.defer {
			presenceOps.values()
		}
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { presenceOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.defer {
			presenceOps.get(presenceId(guildId, userId))
		}
	}
}
