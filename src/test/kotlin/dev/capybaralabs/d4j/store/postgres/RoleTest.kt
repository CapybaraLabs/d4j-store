package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildRoleCreate
import discord4j.discordjson.json.gateway.GuildRoleDelete
import discord4j.discordjson.json.gateway.GuildRoleUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RoleTest {

	@Test
	fun countRoles() {
		assertThat(accessor.countRoles().blockOptional()).isPresent
	}


	@Test
	fun onGuildRoleCreate_createRole() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()

		createRole(guildId, roleId)

		assertThat(accessor.getRoleById(guildId, roleId).block())
			.matches { it.id().asLong() == roleId }
		assertThat(accessor.roles.collectList().block())
			.anyMatch { it.id().asLong() == roleId }
	}

	@Test
	fun onGuildRoleCreate_addToGuild() {
		val guildId = generateUniqueSnowflakeId()
		val roleIdA = generateUniqueSnowflakeId()
		val roleIdB = generateUniqueSnowflakeId()

		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addRoles(role(roleIdA).build()).build())
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		createRole(guildId, roleIdB)

		assertThat(accessor.countRolesInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.roles())
			.hasSize(2)
			.anyMatch { it.asLong() == roleIdA }
			.anyMatch { it.asLong() == roleIdB }
		assertThat(accessor.getRolesInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == roleIdA }
			.anyMatch { it.id().asLong() == roleIdB }
	}


	@Test
	fun onGuildRoleDelete_deleteRole() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()
		createRole(guildId, roleId)


		deleteRole(guildId, roleId)

		assertThat(accessor.getRoleById(guildId, roleId).block()).isNull()
		assertThat(accessor.roles.collectList().block())
			.noneMatch { it.id().asLong() == roleId }
	}

	@Test
	fun onGuildRoleDelete_removeFromMembers() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addRoles(role(roleId).build())
					.addMembers(member(userId).addRole(roleId).build())
					.build()
			)
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getMemberById(guildId, userId).block()!!.roles())
			.anyMatch { it.asLong() == roleId }

		deleteRole(guildId, roleId)

		assertThat(accessor.getMemberById(guildId, userId).block()!!.roles())
			.noneMatch { it.asLong() == roleId }
	}

	@Test
	fun onGuildRoleDelete_removeFromGuild() {
		val guildId = generateUniqueSnowflakeId()
		val roleIdA = generateUniqueSnowflakeId()
		val roleIdB = generateUniqueSnowflakeId()

		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addRoles(role(roleIdA).build()).build())
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		createRole(guildId, roleIdB)

		deleteRole(guildId, roleIdA)

		assertThat(accessor.countRolesInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.getGuildById(guildId).block()!!.roles())
			.hasSize(1)
			.noneMatch { it.asLong() == roleIdA }
			.anyMatch { it.asLong() == roleIdB }
		assertThat(accessor.getRolesInGuild(guildId).collectList().block())
			.hasSize(1)
			.noneMatch { it.id().asLong() == roleIdA }
			.anyMatch { it.id().asLong() == roleIdB }

	}

	// TODO test more properties
	@Test
	fun onGuildRoleUpdate_updateName() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()

		createRole(guildId, roleId)

		assertThat(accessor.getRoleById(guildId, roleId).block())
			.matches { it.id().asLong() == roleId && it.name() == "Ensign" }


		val guildRoleUpdate = GuildRoleUpdate.builder()
			.guildId(guildId)
			.role(role(roleId).name("Captain").build())
			.build()
		updater.onGuildRoleUpdate(0, guildRoleUpdate).block()

		assertThat(accessor.getRoleById(guildId, roleId).block())
			.matches { it.id().asLong() == roleId && it.name() == "Captain" }
	}

	private fun createRole(guildId: Long, roleId: Long) {
		val guildRoleCreate = GuildRoleCreate.builder()
			.guildId(guildId)
			.role(role(roleId).build())
			.build()
		updater.onGuildRoleCreate(0, guildRoleCreate).block()
	}

	private fun deleteRole(guildId: Long, roleId: Long) {
		val guildRoleDelete = GuildRoleDelete.builder()
			.guildId(guildId)
			.roleId(roleId)
			.build()
		updater.onGuildRoleDelete(0, guildRoleDelete).block()
	}
}
