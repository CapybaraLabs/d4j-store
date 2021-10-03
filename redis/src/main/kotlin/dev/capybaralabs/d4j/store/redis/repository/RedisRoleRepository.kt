package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisRoleRepository : RoleRepository {
	override fun save(guildId: Long, role: RoleData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(guildId: Long, roles: List<RoleData>, shardIndex: Int): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteById(roleId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByIds(roleIds: List<Long>): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countRoles(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getRoles(): Flux<RoleData> {
		TODO("Not yet implemented")
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		TODO("Not yet implemented")
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		TODO("Not yet implemented")
	}
}
