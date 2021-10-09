package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface RoleRepository {
	fun save(guildId: Long, role: RoleData, shardIndex: Int): Mono<Void>
	fun saveAll(guildId: Long, roles: List<RoleData>, shardIndex: Int): Mono<Void>

	fun deleteById(roleId: Long): Mono<Int>
	fun deleteByIds(roleIds: List<Long>): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>

	fun countRoles(): Mono<Long>
	fun countRolesInGuild(guildId: Long): Mono<Long>

	fun getRoles(): Flux<RoleData>
	fun getRolesInGuild(guildId: Long): Flux<RoleData>
	fun getRoleById(roleId: Long): Mono<RoleData>
}
