package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildMemberAdd
import discord4j.discordjson.json.gateway.GuildMemberRemove
import discord4j.discordjson.json.gateway.GuildMemberUpdate
import discord4j.discordjson.json.gateway.GuildMembersChunk
import discord4j.discordjson.json.gateway.PresenceUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class MemberTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun countMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userId)
		assertThat(accessor.countMembers().block()!!).isGreaterThan(0)
	}


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
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId }
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
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(2)
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
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(1)
			.anyMatch { it.asLong() == userIdA }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(1)
			.anyMatch { it.user().id().asLong() == userIdA }
	}

	private fun removeMember(guildId: Long, userId: Long) {
		val guildMemberRemove = GuildMemberRemove.builder()
			.guildId(guildId)
			.user(user(userId).build())
			.build()
		updater.onGuildMemberRemove(0, guildMemberRemove).block()
	}

	@Test
	fun onGuildMembersChunk_createMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		guildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getMemberById(guildId, userIdA).block())
			.matches { it.user().id().asLong() == userIdA }
		assertThat(accessor.getMemberById(guildId, userIdB).block())
			.matches { it.user().id().asLong() == userIdB }
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}

	@Test
	fun onGuildMembersChunk_createUsers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		guildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getUserById(userIdA).block())
			.matches { it.id().asLong() == userIdA }
		assertThat(accessor.getUserById(userIdB).block())
			.matches { it.id().asLong() == userIdB }

		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userIdA }
			.anyMatch { it.id().asLong() == userIdB }
	}

	@Test
	fun onGuildMembersChunk_addToGuild() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val userIdC = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userIdA)
		guildMembersChunk(guildId, userIdB, userIdC)

		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(3)
			.anyMatch { it.asLong() == userIdA }
			.anyMatch { it.asLong() == userIdB }
			.anyMatch { it.asLong() == userIdC }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
			.anyMatch { it.user().id().asLong() == userIdC }
	}

	@Test
	fun onGuildMembersChunk_createOfflinePresences() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		guildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getPresenceById(guildId, userIdA).block())
			.matches { it.user().id().asLong() == userIdA && it.status() == "offline" }
		assertThat(accessor.getPresenceById(guildId, userIdB).block())
			.matches { it.user().id().asLong() == userIdB && it.status() == "offline" }

		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA && it.status() == "offline" }
			.anyMatch { it.user().id().asLong() == userIdB && it.status() == "offline" }
	}

	private fun guildMembersChunk(guildId: Long, vararg userIds: Long) {
		val guildMembersChunk = GuildMembersChunk.builder()
			.guildId(guildId)
			.chunkIndex(0)
			.chunkCount(1)
			.addAllMembers(
				userIds.map { member(it).build() }
			)
			.build()

		updater.onGuildMembersChunk(0, guildMembersChunk).block()
	}

	@Test
	fun onGuildMemberUpdate_updateNick() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userId)

		val guildMemberUpdate = GuildMemberUpdate.builder()
			.guildId(guildId)
			.nickOrNull("Neelix")
			.user(user(userId).build())
			.build()
		updater.onGuildMemberUpdate(0, guildMemberUpdate).block()

		assertThat(accessor.getMemberById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId && it.nick().get().get() == "Neelix" }
	}

	@Test
	fun onGuildMembersCompletion_doNothing() {
		val guildId = generateUniqueSnowflakeId()

		// ¯\_(ツ)_/¯
		assertThat(updater.onGuildMembersCompletion(guildId).block()).isNull()
	}


	private val noop = storeLayoutProvider.withFlags(StoreFlag.allBut(StoreFlag.MEMBER))
	private val noopAccessor = noop.dataAccessor
	private val noopUpdater = noop.gatewayDataUpdater

	@Test
	fun givenNoMemberStoreFlag_countIsZero() {
		// TODO delete
		// TODO delete shard

		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userId)
		assertThat(noopAccessor.countMembers().block()!!).isZero
		assertThat(noopAccessor.countExactMembersInGuild(guildId).block()!!).isZero
		assertThat(noopAccessor.countMembersInGuild(guildId).block()!!).isZero

		assertThat(noopAccessor.getExactMembersInGuild(guildId).collectList().block()).isEmpty()
		assertThat(noopAccessor.getMemberById(guildId, userId).block()).isNull()
	}

	@Test
	fun givenNoMemberStoreFlag_membersIsEmpty() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userId)

		assertThat(noopAccessor.members.collectList().block()!!).isEmpty()
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberAdd_doNotCreateMember() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).build())
			.build()

		noopUpdater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getMemberById(guildId, userId).blockOptional()).isEmpty
		assertThat(accessor.members.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberAdd_createUser() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).build())
			.build()

		noopUpdater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberAdd_addToGuild_doNotCreateMember() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userIdA)

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userIdB).build())
			.build()
		noopUpdater.onGuildMemberAdd(0, guildMemberAdd).block()


		// TODO this sounds buggy. lets change the code to use the guild in more places to calculate numbers of stuff inside of it
		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(2)
			.anyMatch { it.asLong() == userIdA }
			.anyMatch { it.asLong() == userIdB }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(1)
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberRemove_keepMember() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createGuildWithMembers(guildId, userId)

		assertThat(accessor.getMemberById(guildId, userId).block()).isNotNull
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }


		noopRemoveMember(guildId, userId)

		assertThat(accessor.getMemberById(guildId, userId).block()).isNotNull
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberRemove_deleteOrphanedUser() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userId)
		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }


		noopRemoveMember(guildId, userId)

		assertThat(accessor.getUserById(userId).block()).isNull()
	}

	@Disabled("Figure out a way to deal with deleting orphaned users or avoid doing it when MemberRepo is noop")
	@Test
	fun givenNoMemberStoreFlag_onGuildMemberRemove_keepNonOrphanedUser() {
		val guildIdA = generateUniqueSnowflakeId()
		val guildIdB = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildIdA, userId)
		createGuildWithMembers(guildIdB, userId)

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }


		noopRemoveMember(guildIdA, userId)

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberRemove_deletePresence() {
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


		noopRemoveMember(guildId, userId)
		assertThat(accessor.getPresenceById(guildId, userId).block()).isNull()
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberRemove_removeFromGuild_butKeepMember() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userIdA, userIdB)

		noopRemoveMember(guildId, userIdB)

		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(1)
			.anyMatch { it.asLong() == userIdA }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}

	private fun noopRemoveMember(guildId: Long, userId: Long) {
		val guildMemberRemove = GuildMemberRemove.builder()
			.guildId(guildId)
			.user(user(userId).build())
			.build()
		noopUpdater.onGuildMemberRemove(0, guildMemberRemove).block()
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMembersChunk_dontCreateMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		noopGuildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getMemberById(guildId, userIdA).blockOptional()).isEmpty
		assertThat(accessor.getMemberById(guildId, userIdB).blockOptional()).isEmpty
		assertThat(accessor.members.collectList().block())
			.noneMatch { it.user().id().asLong() == userIdA }
			.noneMatch { it.user().id().asLong() == userIdB }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMembersChunk_createUsers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		noopGuildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getUserById(userIdA).block())
			.matches { it.id().asLong() == userIdA }
		assertThat(accessor.getUserById(userIdB).block())
			.matches { it.id().asLong() == userIdB }

		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userIdA }
			.anyMatch { it.id().asLong() == userIdB }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMembersChunk_addToGuild_doNotCreateMember() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val userIdC = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userIdA)
		noopGuildMembersChunk(guildId, userIdB, userIdC)

		assertThat(accessor.countMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.countExactMembersInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.getGuildById(guildId).block()!!.members())
			.hasSize(3)
			.anyMatch { it.asLong() == userIdA }
			.anyMatch { it.asLong() == userIdB }
			.anyMatch { it.asLong() == userIdC }
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(1)
			.anyMatch { it.user().id().asLong() == userIdA }
			.noneMatch { it.user().id().asLong() == userIdB }
			.noneMatch { it.user().id().asLong() == userIdC }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMembersChunk_createOfflinePresences() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		noopGuildMembersChunk(guildId, userIdA, userIdB)

		assertThat(accessor.getPresenceById(guildId, userIdA).block())
			.matches { it.user().id().asLong() == userIdA && it.status() == "offline" }
		assertThat(accessor.getPresenceById(guildId, userIdB).block())
			.matches { it.user().id().asLong() == userIdB && it.status() == "offline" }

		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA && it.status() == "offline" }
			.anyMatch { it.user().id().asLong() == userIdB && it.status() == "offline" }
	}

	private fun noopGuildMembersChunk(guildId: Long, vararg userIds: Long) {
		val guildMembersChunk = GuildMembersChunk.builder()
			.guildId(guildId)
			.chunkIndex(0)
			.chunkCount(1)
			.addAllMembers(
				userIds.map { member(it).build() }
			)
			.build()

		noopUpdater.onGuildMembersChunk(0, guildMembersChunk).block()
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMemberUpdate_doNotUpdateNick() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createGuildWithMembers(guildId, userId)

		val guildMemberUpdate = GuildMemberUpdate.builder()
			.guildId(guildId)
			.nickOrNull("Neelix")
			.user(user(userId).build())
			.build()
		noopUpdater.onGuildMemberUpdate(0, guildMemberUpdate).block()

		assertThat(accessor.getMemberById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId && it.nick().isAbsent }
	}

	@Test
	fun givenNoMemberStoreFlag_onGuildMembersCompletion_doNothing() {
		val guildId = generateUniqueSnowflakeId()

		// ¯\_(ツ)_/¯
		assertThat(updater.onGuildMembersCompletion(guildId).block()).isNull()
	}

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
}
