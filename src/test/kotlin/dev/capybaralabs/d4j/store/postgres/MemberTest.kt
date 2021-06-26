package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildMemberAdd
import discord4j.discordjson.json.gateway.GuildMemberRemove
import discord4j.discordjson.json.gateway.PresenceUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MemberTest {

	@Test
	fun onGuildMemberAdd_createMember() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).build())
			.build()

		updater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getMemberById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId }
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onGuildMemberAdd_createUser() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).build())
			.build()

		updater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }
	}

	@Test
	fun onGuildMemberAdd_addToGuild() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userIdA)

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userIdB).build())
			.build()
		updater.onGuildMemberAdd(0, guildMemberAdd).block()


		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(2)
			.anyMatch { it.asLong() == userIdA }
			.anyMatch { it.asLong() == userIdB }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}


	@Test
	fun onGuildMemberRemove_deleteMember() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userId)

		assertThat(accessor.getMemberById(guildId, userId).block()).isNotNull
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }


		removeMember(guildId, userId)

		assertThat(accessor.getMemberById(guildId, userId).block()).isNull()
		assertThat(accessor.members.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onGuildMemberRemove_deleteOrphanedUser() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userId)
		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }


		removeMember(guildId, userId)

		assertThat(accessor.getUserById(userId).block()).isNull()
	}

	@Test
	fun onGuildMemberRemove_keepNonOrphanedUser() {
		val guildIdA = generateUniqueSnowflakeId()
		val guildIdB = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildIdA, userId)
		createGuildWithMembers(guildIdB, userId)

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }


		removeMember(guildIdA, userId)

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }
	}

	@Test
	fun onGuildMemberRemove_deletePresence() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate).block()
		assertThat(accessor.getPresenceById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId }


		removeMember(guildId, userId)
		assertThat(accessor.getPresenceById(guildId, userId).block()).isNull()
	}

	@Test
	fun onGuildMemberRemove_removeFromGuild() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userIdA, userIdB)

		removeMember(guildId, userIdB)

		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(1)
			.anyMatch { it.asLong() == userIdA }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(1)
			.anyMatch { it.user().id().asLong() == userIdA }
	}

	// TODO onGuildMembersChunk

	// TODO onGuildMemberUpdate

	// TODO onGuildMembersCompletion


	private fun createGuildWithMembers(guildId: Long, vararg userIds: Long) {
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId).addAllMembers(
					userIds.map { member(it).build() }
				).build()
			)
			.build()
		updater.onGuildCreate(0, guildCreate).block()
	}

	private fun removeMember(guildId: Long, userId: Long) {
		val guildMemberRemove = GuildMemberRemove.builder()
			.guildId(guildId)
			.user(user(userId).build())
			.build()
		updater.onGuildMemberRemove(0, guildMemberRemove).block()
	}
}
