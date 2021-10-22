package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import discord4j.discordjson.json.RoleData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopRoleRepository : RoleRepository {
	override fun save(guildId: Long, role: RoleData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(rolesByGuild: Map<Long, List<RoleData>>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteById(roleId: Long, guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countRoles(): Mono<Long> {
		return Mono.empty()
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.empty()
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return Flux.empty()
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.empty()
	}
}
