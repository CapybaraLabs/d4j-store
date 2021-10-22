package dev.capybaralabs.d4j.store.tck

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
			.user(
				user(userId)
					.username("Locutus of Borg")
					.build()
			)
			.build()
		updater.onUserUpdate(0, userUpdate).block()

		assertThat(accessor.getUserById(userId).block())
			.matches { it.id().asLong() == userId && it.username() == "Locutus of Borg" }
		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userId && it.username() == "Locutus of Borg" }
	}
}
