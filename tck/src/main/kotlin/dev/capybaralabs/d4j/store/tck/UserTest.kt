package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import discord4j.discordjson.json.gateway.GuildMemberAdd
import discord4j.discordjson.json.gateway.UserUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UserTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun countUsers() {
		assertThat(accessor.countUsers().blockOptional()).isPresent
	}


	// TODO test more properties being updated
	@Test
	fun onUserUpdate_updateName() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).user(user(userId).username("Jean-Luc Picard").build()).build())
			.build()
		updater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }


		val userUpdate = UserUpdate.builder()
			.user(user(userId).username("Locutus of Borg").build())
			.build()
		updater.onUserUpdate(0, userUpdate).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Locutus of Borg" }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId && it.username() == "Locutus of Borg" }
	}


	private val noop = storeLayoutProvider.withFlags(StoreFlag.allBut(StoreFlag.USER))
	private val noopAccessor = noop.dataAccessor
	private val noopUpdater = noop.gatewayDataUpdater

	@Test
	fun givenNoUserStoreFlag_countIsZero() {
		// TODO delete
		// TODO delete shard
		assertThat(noopAccessor.countUsers().block()!!).isZero
	}

	@Test
	fun givenNoUserStoreFlag_usersIsEmpty() {
		assertThat(noopAccessor.users.collectList().block()!!).isEmpty()
	}


	@Test
	fun givenNoUserStoreFlagAndUserNotExists_onUserUpdate_doNotCreateUser() {
		val userId = generateUniqueSnowflakeId()

		assertThat(accessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(accessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }

		assertThat(noopAccessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(noopAccessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }


		val userUpdate = UserUpdate.builder()
			.user(user(userId).username("Locutus of Borg").build())
			.build()
		assertThat(noopUpdater.onUserUpdate(0, userUpdate).blockOptional()).isEmpty

		assertThat(accessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(accessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }

		assertThat(noopAccessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(noopAccessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }
	}

	@Test
	fun givenNoUserStoreFlag_onUserUpdate_doNotUpdateUser() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val guildMemberAdd = GuildMemberAdd.builder()
			.guildId(guildId)
			.member(member(userId).user(user(userId).username("Jean-Luc Picard").build()).build())
			.build()
		updater.onGuildMemberAdd(0, guildMemberAdd).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }

		assertThat(noopAccessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(noopAccessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }


		val userUpdate = UserUpdate.builder()
			.user(user(userId).username("Locutus of Borg").build())
			.build()
		assertThat(noopUpdater.onUserUpdate(0, userUpdate).blockOptional()).isEmpty

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }

		assertThat(noopAccessor.getUserById(userId).blockOptional()).isEmpty
		assertThat(noopAccessor.users.collectList().block())
			.noneMatch { it.id().asLong() == userId && it.username() == "Jean-Luc Picard" }
	}

}
