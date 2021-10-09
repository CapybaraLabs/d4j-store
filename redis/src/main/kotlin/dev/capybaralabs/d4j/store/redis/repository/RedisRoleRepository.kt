package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisRoleRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), RoleRepository {

	private val hash = key("role")
	private val hashOps = factory.createRedisHashOperations<String, Long, RoleData>()

	override fun save(guildId: Long, role: RoleData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(role), shardIndex)
	}

	override fun saveAll(guildId: Long, roles: List<RoleData>, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			hashOps.putAll(hash, roles.associateBy { it.id().asLong() }).then()
		}
	}

	override fun deleteById(roleId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, roleId)
				.map { StrictMath.toIntExact(it) }
		}
	}

	override fun deleteByIds(roleIds: List<Long>): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, *roleIds.toTypedArray())
				.map { StrictMath.toIntExact(it) }
		}
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countRoles(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		TODO("Not yet implemented")
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.defer {
			hashOps.get(hash, roleId)
		}
	}
}
