package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.VoiceStateData
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

		assertThat(accessor.guilds.collectList().block())
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
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == channelIdA }
			.anyMatch { it.id().asLong() == channelIdB }
		assertThat(accessor.channels.collectList().block())
			.anyMatch { it.id().asLong() == channelIdA }
			.anyMatch { it.id().asLong() == channelIdB }
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
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().get().asLong() == emojiIdA }
			.anyMatch { it.id().get().asLong() == emojiIdB }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA }
			.anyMatch { it.id().get().asLong() == emojiIdB }
	}

	@Test
	fun onGuildCreate_createMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.memberCount(2)
					.approximateMemberCount(3)
					.addMembers(
						member(userIdA).build(),
						member(userIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val memberA = accessor.getMemberById(guildId, userIdA).block()!!
		assertThat(memberA.user().id().asLong()).isEqualTo(userIdA)

		val memberB = accessor.getMemberById(guildId, userIdB).block()!!
		assertThat(memberB.user().id().asLong()).isEqualTo(userIdB)

		val count = accessor.countMembersInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.members()).hasSize(2)
		assertThat(guild.memberCount()).isEqualTo(2)
		assertThat(guild.approximateMemberCount().get()).isEqualTo(3)
		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}

	@Test
	fun onGuildCreate_createPresences() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.approximatePresenceCount(2)
					.addPresences(
						presence(userIdA).build(),
						presence(userIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val presenceA = accessor.getPresenceById(guildId, userIdA).block()!!
		assertThat(presenceA.user().id().asLong()).isEqualTo(userIdA)

		val presenceB = accessor.getPresenceById(guildId, userIdB).block()!!
		assertThat(presenceB.user().id().asLong()).isEqualTo(userIdB)

		val count = accessor.countPresencesInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.approximatePresenceCount().get()).isEqualTo(2)
		assertThat(accessor.getPresencesInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}

	@Test
	fun onGuildCreate_createRoles() {
		val guildId = generateUniqueSnowflakeId()
		val roleIdA = generateUniqueSnowflakeId()
		val roleIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addRoles(
						role(roleIdA).build(),
						role(roleIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val roleA = accessor.getRoleById(guildId, roleIdA).block()!!
		assertThat(roleA.id().asLong()).isEqualTo(roleIdA)

		val roleB = accessor.getRoleById(guildId, roleIdB).block()!!
		assertThat(roleB.id().asLong()).isEqualTo(roleIdB)

		val count = accessor.countRolesInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.roles()).hasSize(2)
		assertThat(accessor.getRolesInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == roleIdA }
			.anyMatch { it.id().asLong() == roleIdB }
		assertThat(accessor.roles.collectList().block())
			.anyMatch { it.id().asLong() == roleIdA }
			.anyMatch { it.id().asLong() == roleIdB }
	}

	@Test
	fun onGuildCreate_createUsers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.memberCount(2)
					.approximateMemberCount(3)
					.addMembers(
						member(userIdA).build(),
						member(userIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val userA = accessor.getUserById(userIdA).block()!!
		assertThat(userA.id().asLong()).isEqualTo(userIdA)

		val userB = accessor.getUserById(userIdB).block()!!
		assertThat(userB.id().asLong()).isEqualTo(userIdB)

		assertThat(accessor.users.collectList().block())
			.anyMatch { it.id().asLong() == userIdA }
			.anyMatch { it.id().asLong() == userIdB }
	}

	@Test
	fun onGuildCreate_createVoiceStates() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val userIdC = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addChannels(
						channel(channelIdA).build(),
						channel(channelIdB).build(),
					)
					.addVoiceStates(
						voiceState(guildId, channelIdA, userIdA).build(),
						voiceState(guildId, channelIdB, userIdB).build(),
						voiceState(guildId, channelIdB, userIdC).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val voiceStateA = accessor.getVoiceStateById(guildId, userIdA).block()!!
		assertThat(voiceStateA.guildId().get().asLong()).isEqualTo(guildId)
		assertThat(voiceStateA.channelId().get().asLong()).isEqualTo(channelIdA)
		assertThat(voiceStateA.userId().asLong()).isEqualTo(userIdA)

		val voiceStateB = accessor.getVoiceStateById(guildId, userIdB).block()!!
		assertThat(voiceStateB.guildId().get().asLong()).isEqualTo(guildId)
		assertThat(voiceStateB.channelId().get().asLong()).isEqualTo(channelIdB)
		assertThat(voiceStateB.userId().asLong()).isEqualTo(userIdB)

		val voiceStateC = accessor.getVoiceStateById(guildId, userIdC).block()!!
		assertThat(voiceStateC.guildId().get().asLong()).isEqualTo(guildId)
		assertThat(voiceStateC.channelId().get().asLong()).isEqualTo(channelIdB)
		assertThat(voiceStateC.userId().asLong()).isEqualTo(userIdC)

		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!).isEqualTo(1)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!).isEqualTo(2)

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block())
			.hasSize(1)
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block())
			.hasSize(2)
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.voiceStates.collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))
	}

	private fun isVoiceState(guildId: Long, channelId: Long, userId: Long): (VoiceStateData) -> Boolean {
		return {
			it.guildId().get().asLong() == guildId
				&& it.channelId().get().asLong() == channelId
				&& it.userId().asLong() == userId
		}
	}


	// TODO offline presences test
	// TODO test roles+members
}
