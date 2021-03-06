package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisRoleRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), RoleRepository {

	private val roleKey = key("role")
	private val roleOps = RedisHashOps(roleKey, factory, Long::class.java, RoleData::class.java)

	private val shardIndex = twoWayIndex("$roleKey:shard-index", factory)
	private val guildIndex = oneWayIndex("$roleKey:guild-index", factory)
	private val gShardIndex = twoWayIndex("$roleKey:guild-shard-index", factory)

	override fun save(guildId: Long, role: RoleData, shardId: Int): Mono<Void> {
		return saveAll(mapOf(Pair(guildId, listOf(role))), shardId)
	}

	override fun saveAll(rolesByGuild: Map<Long, List<RoleData>>, shardId: Int): Mono<Void> {
		val filtered = rolesByGuild.filter { it.value.isNotEmpty() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		val ids = filtered.flatMap { it.value }.map { it.id().asLong() }
		val roleMap = filtered.flatMap { it.value }.associateBy { it.id().asLong() }
		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, ids)
			val addToGuildIndex = Flux.fromIterable(
				filtered
					.map { guildIndex.addElements(it.key, *it.value.map { role -> role.id().asLong() }.toTypedArray()) }
			).flatMap { it }
			val addToGuildShardIndex = gShardIndex.addElements(shardId, filtered.keys)

			val save = roleOps.putAll(roleMap)

			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, save)
		}
	}

	override fun deleteById(roleId: Long, guildId: Long): Mono<Long> {
		return Mono.defer {
			val remove = roleOps.remove(roleId)
			val removeFromShardIndex = shardIndex.removeElements(roleId)
			val removeFromGuildIndex = guildIndex.removeElements(guildId, roleId)

			Mono.`when`(removeFromShardIndex, removeFromGuildIndex)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { roleIdsInGuild ->
					val removeFromShardIndex = shardIndex.removeElements(*roleIdsInGuild.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = roleOps.remove(*roleIdsInGuild.toTypedArray())

					Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val getIds: Mono<Set<Long>> = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
			val getGuildIds: Mono<Set<Long>> = gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { gShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }

			val getAllIds = getGuildIds.flatMap { guildIds ->
				// Technically we don't need to fetch the ids from the groups here, we can rely on the shard index only.
				guildIndex.getElementsInGroups(guildIds).collectSet()
					.flatMap { guildIndex.deleteGroups(guildIds).then(Mono.just(it)) }
					.flatMap { idsInGuilds -> getIds.map { ids -> idsInGuilds + ids } }
			}

			getAllIds.flatMap { roleOps.remove(*it.toTypedArray()) }
		}
	}

	override fun countRoles(): Mono<Long> {
		return Mono.defer {
			roleOps.size()
		}
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.defer {
			roleOps.values()
		}
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { roleOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.defer {
			roleOps.get(roleId)
		}
	}
}
