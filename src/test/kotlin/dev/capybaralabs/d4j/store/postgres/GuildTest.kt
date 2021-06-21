package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.possible.Possible
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GuildTest {

	// TODO fun onGuildDelete_deleteMessageInChannels()

	@Test
	fun onGuildCreate_createGuild() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()

		updater.onGuildCreate(0, guildCreate).blockOptional()


		val guild = accessor.getGuildById(guildId).block()!!

		assertThat(guild.id().asLong()).isEqualTo(guildId)
		assertThat(guild.name()).isEqualTo("Deep Space 9")

		assertThat(accessor.guilds.collectList().block()!!)
			.anyMatch { it.id().asLong() == guildId }
	}

	@Test
	fun onGuildCreate_createChannels() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addChannels(
						// simulating real payloads here, they appear to not have guild ids set
						channel(channelIdA).guildId(Possible.absent()).build(),
						channel(channelIdB).guildId(Possible.absent()).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val channelA = accessor.getChannelById(channelIdA).block()!!
		assertThat(channelA.id().asLong()).isEqualTo(channelIdA)
		assertThat(channelA.guildId().get().asLong()).isEqualTo(guildId)

		val channelB = accessor.getChannelById(channelIdB).block()!!
		assertThat(channelB.id().asLong()).isEqualTo(channelIdB)
		assertThat(channelB.guildId().get().asLong()).isEqualTo(guildId)

		val count = accessor.countChannelsInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.channels()).hasSize(2)
		val channelsInGuild = accessor.getChannelsInGuild(guildId).collectList().block()!!
		assertThat(channelsInGuild)
			.hasSize(2)
			.anySatisfy { assertThat(it.id().asLong()).isEqualTo(channelIdA) }
			.anySatisfy { assertThat(it.id().asLong()).isEqualTo(channelIdB) }
	}

	@Test
	fun onGuildCreate_createEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).build(),
						emoji(emojiIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val emojiA = accessor.getEmojiById(guildId, emojiIdA).block()!!
		assertThat(emojiA.id().get().asLong()).isEqualTo(emojiIdA)

		val emojiB = accessor.getEmojiById(guildId, emojiIdB).block()!!
		assertThat(emojiB.id().get().asLong()).isEqualTo(emojiIdB)

		val count = accessor.countEmojisInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.emojis()).hasSize(2)
		val emojisInGuild = accessor.getEmojisInGuild(guildId).collectList().block()!!
		assertThat(emojisInGuild)
			.hasSize(2)
			.anySatisfy { assertThat(it.id().get().asLong()).isEqualTo(emojiIdA) }
			.anySatisfy { assertThat(it.id().get().asLong()).isEqualTo(emojiIdB) }
	}
}
