package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.RoleData
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisRoleRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), RoleRepository {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val roleKey = key("role")
	private val roleOps = factory.createRedisHashOperations<String, Long, RoleData>()

	private val shardIndex = twoWayIndex("$roleKey:shard-index", factory)
	private val guildIndex = oneWayIndex("$roleKey:guild-index", factory)
	private val gShardIndex = twoWayIndex("$roleKey:guild-shard-index", factory)

	override fun save(guildId: Long, role: RoleData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(role), shardId)
	}

	override fun saveAll(guildId: Long, roles: List<RoleData>, shardId: Int): Mono<Void> {
		if (roles.isEmpty()) {
			return Mono.empty()
		}

		val ids = roles.map { it.id().asLong() }
		return Mono.defer {
			val addToShardIndex = shardIndex.addElements(shardId, ids)
			val addToGuildIndex = guildIndex.addElements(guildId, *ids.toTypedArray())
			val addToGuildShardIndex = gShardIndex.addElements(shardId, listOf(guildId))

			val save = roleOps.putAll(roleKey, roles.associateBy { it.id().asLong() }).then()

			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, save)
		}
	}

	override fun deleteById(roleId: Long, guildId: Long): Mono<Long> {
		return Mono.defer {
			val remove = roleOps.remove(roleKey, roleId)
			val removeFromShardIndex = shardIndex.removeElements(roleId)
			val removeFromGuildIndex = guildIndex.removeElements(guildId, roleId)

			Mono.`when`(removeFromShardIndex, removeFromGuildIndex)
				.then(remove)
		}
	}

	override fun deleteByGuildId(roleIds: List<Long>, guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { idsInGuild ->
					if (roleIds != idsInGuild) {
						log.warn("Guild index deviates from ids parameter: {} vs {}", idsInGuild, roleIds)
					}
					val allIds = roleIds + idsInGuild

					val removeFromShardIndex = shardIndex.removeElements(*allIds.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = roleOps.remove(roleKey, *allIds.toTypedArray())

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

			getAllIds.flatMap { roleOps.remove(roleKey, *it.toTypedArray()) }
		}
	}

	override fun countRoles(): Mono<Long> {
		return Mono.defer {
			roleOps.size(roleKey)
		}
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.defer {
			roleOps.values(roleKey)
		}
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { roleOps.multiGet(roleKey, it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.defer {
			roleOps.get(roleKey, roleId)
		}
	}
}
