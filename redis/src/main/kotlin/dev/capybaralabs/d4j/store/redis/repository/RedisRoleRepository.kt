package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisRoleRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), RoleRepository {

	private val roleKey = key("role")
	private val roleOps = factory.createRedisHashOperations<String, Long, RoleData>()

	override fun save(guildId: Long, role: RoleData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(role), shardId)
	}

	override fun saveAll(guildId: Long, roles: List<RoleData>, shardId: Int): Mono<Void> {
		return Mono.defer {
			roleOps.putAll(roleKey, roles.associateBy { it.id().asLong() }).then()
		}
	}

	override fun deleteById(roleId: Long): Mono<Long> {
		return Mono.defer {
			roleOps.remove(roleKey, roleId)
		}
	}

	override fun deleteByGuildId(roleIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			roleOps.remove(roleKey, *roleIds.toTypedArray())
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countRoles(): Mono<Long> {
		return Mono.defer {
			roleOps.size(roleKey)
		}
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.defer {
			roleOps.values(roleKey)
		}
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		TODO("Not yet implemented")
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.defer {
			roleOps.get(roleKey, roleId)
		}
	}
}
