package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.gateway.PresenceUpdate
import discord4j.discordjson.json.gateway.UserUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PresenceTest {

	// TODO update more presence properties
	@Test
	fun onPresenceUpdate_updatePresenceStatus() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		val presenceUpdate1 = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate1).block()

		assertThat(accessor.getPresenceById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId && it.status() == "online" }
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userId && it.status() == "online" }

		val presenceUpdate2 = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).build())
			.status("dnd")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate2).block()

		assertThat(accessor.getPresenceById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId && it.status() == "dnd" }
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userId && it.status() == "dnd" }
	}


	@Test
	fun onPresenceUpdate_updateUsername() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createUser(user(userId).username("Jean-Luc Picard").build())

		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).username("Locutus of Borg").build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Locutus of Borg" }
	}

	@Test
	fun onPresenceUpdate_updateUserDiscriminator() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createUser(user(userId).discriminator("0001").build())

		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).discriminator("0002").build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.discriminator() == "0002" }
	}

	@Test
	fun onPresenceUpdate_updateUserAvatar() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()

		createUser(user(userId).avatar("human.jpeg").build())

		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).avatar("borg.png").build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(0, presenceUpdate).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.avatar().get() == "borg.png" }

	}

	private fun createUser(userData: UserData) {
		val userUpdate = UserUpdate.builder()
			.user(userData)
			.build()
		updater.onUserUpdate(0, userUpdate).block()
	}
}
