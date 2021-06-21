package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.possible.Possible
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GuildTest {

	// TODO fun onGuildDelete_deleteMessageInChannels()

	@Test
	fun onGuildCreate() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(ds9Guild(guildId).build())
			.build()

		updater.onGuildCreate(0, guildCreate).blockOptional()


		val guild = accessor.getGuildById(guildId).block()!!

		assertThat(guild.id().asLong()).isEqualTo(guildId)
		assertThat(guild.name()).isEqualTo("Deep Space 9")
	}

	@Test
	fun channelsInGuild() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				ds9Guild(guildId)
					.addChannels(
						// simulating real payloads here, they appear to not have guild ids set
						channel(channelIdA).guildId(Possible.absent()).build(),
						channel(channelIdB).guildId(Possible.absent()).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.channels()).hasSize(2)

		val channelA = accessor.getChannelById(channelIdA).block()!!
		assertThat(channelA.id().asLong()).isEqualTo(channelIdA)
		assertThat(channelA.guildId().get().asLong()).isEqualTo(guildId)

		val channelB = accessor.getChannelById(channelIdB).block()!!
		assertThat(channelB.id().asLong()).isEqualTo(channelIdB)
		assertThat(channelB.guildId().get().asLong()).isEqualTo(guildId)

		val count = accessor.countChannelsInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)
		val channelsInGuild = accessor.getChannelsInGuild(guildId).collectList().block()!!
		assertThat(channelsInGuild)
			.hasSize(2)
			.anySatisfy { assertThat(it.id().asLong()).isEqualTo(channelIdA) }
			.anySatisfy { assertThat(it.id().asLong()).isEqualTo(channelIdB) }
	}

	@Test
	fun emojisInGuild() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				ds9Guild(guildId)
					.addEmojis(
						emoji(emojiIdA).build(),
						emoji(emojiIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val count = accessor.countEmojisInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.emojis()).hasSize(2)

		val emojiA = accessor.getEmojiById(guildId, emojiIdA).block()!!
		assertThat(emojiA.id().get().asLong()).isEqualTo(emojiIdA)

		val emojiB = accessor.getEmojiById(guildId, emojiIdB).block()!!
		assertThat(emojiB.id().get().asLong()).isEqualTo(emojiIdB)
	}
}
