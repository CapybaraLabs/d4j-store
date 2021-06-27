package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildMembersChunk
import discord4j.discordjson.json.gateway.GuildUpdate
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.possible.Possible
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GuildTest {

	@Test
	fun countGuilds() {
		assertThat(accessor.countGuilds().blockOptional()).isPresent
	}


	@Test
	fun onGuildCreate_createGuild() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()

		updater.onGuildCreate(0, guildCreate).block()


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
	fun onGuildCreate_silentlyDropEmojisWithoutId() {
		val guildId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiId).build(),
						unicodeEmoji("🖖").build(),
					)
					.build()
			)
			.build()
		updater.onGuildCreate(0, guildCreate).block()


		assertThat(accessor.getEmojiById(guildId, emojiId).block())
			.matches { it.id().get().asLong() == emojiId }

		val count = accessor.countEmojisInGuild(guildId).block()!!
		assertThat(count).isEqualTo(1)

		assertThat(accessor.getGuildById(guildId).block()!!.emojis()).hasSize(1)
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(1)
			.anyMatch { it.id().get().asLong() == emojiId }
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
		assertThat(guild.members())
			.hasSize(2)
			.anyMatch { it.asLong() == userIdA }
			.anyMatch { it.asLong() == userIdB }
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
		assertThat(presenceA.status()).isEqualTo("online")

		val presenceB = accessor.getPresenceById(guildId, userIdB).block()!!
		assertThat(presenceB.user().id().asLong()).isEqualTo(userIdB)
		assertThat(presenceB.status()).isEqualTo("online")

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
	fun onGuildCreate_createOfflinePresences() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.approximatePresenceCount(2)
					.addMembers(
						member(userIdA).build(),
						member(userIdB).build(),
					)
					.addPresences(
						presence(userIdA).status("dnd").build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val presenceA = accessor.getPresenceById(guildId, userIdA).block()!!
		assertThat(presenceA.user().id().asLong()).isEqualTo(userIdA)
		assertThat(presenceA.status()).isEqualTo("dnd")

		val presenceB = accessor.getPresenceById(guildId, userIdB).block()!!
		assertThat(presenceB.user().id().asLong()).isEqualTo(userIdB)
		assertThat(presenceB.status()).isEqualTo("offline")

		val count = accessor.countPresencesInGuild(guildId).block()!!
		assertThat(count).isEqualTo(2)

		val guild = accessor.getGuildById(guildId).block()!!
		assertThat(guild.approximatePresenceCount().get()).isEqualTo(2)
		assertThat(accessor.getPresencesInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA && it.status() == "dnd" }
			.anyMatch { it.user().id().asLong() == userIdB && it.status() == "offline" }
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userIdA && it.status() == "dnd" }
			.anyMatch { it.user().id().asLong() == userIdB && it.status() == "offline" }
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
						voiceStateInChannel(guildId, channelIdA, userIdA).build(),
						voiceStateInChannel(guildId, channelIdB, userIdB).build(),
						voiceStateInChannel(guildId, channelIdB, userIdC).build(),
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

	@Test
	fun onGuildCreate_silentlyDropVoiceStatesWithoutChannelId() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addChannels(
						channel(channelIdA).build(),
						channel(channelIdB).build(),
					)
					.addVoiceStates(
						voiceStateInChannel(guildId, channelIdA, userIdA).build(),
						voiceStateNoChannel(guildId, userIdB).build(),
					)
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()


		val voiceStateA = accessor.getVoiceStateById(guildId, userIdA).block()!!
		assertThat(voiceStateA.guildId().get().asLong()).isEqualTo(guildId)
		assertThat(voiceStateA.channelId().get().asLong()).isEqualTo(channelIdA)
		assertThat(voiceStateA.userId().asLong()).isEqualTo(userIdA)

		assertThat(accessor.getVoiceStateById(guildId, userIdB).block()).isNull()
		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!).isEqualTo(1)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!).isEqualTo(1)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!).isEqualTo(0)

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block())
			.hasSize(1)
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdB))

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block())
			.hasSize(1)
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block())
			.hasSize(0)
			.noneMatch(isVoiceState(guildId, channelIdB, userIdB))

		assertThat(accessor.voiceStates.collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdB))
	}


	@Test
	fun onGuildDelete_deleteGuild() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getGuildById(guildId).block()).isNotNull
		assertThat(accessor.guilds.collectList().block()).anyMatch { it.id().asLong() == guildId }


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getGuildById(guildId).block()).isNull()
		assertThat(accessor.guilds.collectList().block()).noneMatch { it.id().asLong() == guildId }
	}

	@Test
	fun onGuildDelete_deleteChannels() {
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addChannels(channel(channelId).guildId(Possible.absent()).build()).build())
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getChannelById(channelId).block()).isNotNull
		assertThat(accessor.countChannelsInGuild(guildId).block()!!).isOne
		assertThat(accessor.getGuildById(guildId).block()!!.channels()).hasSize(1)
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block()).hasSize(1)
		assertThat(accessor.channels.collectList().block())
			.anyMatch { it.id().asLong() == channelId }

		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getChannelById(channelId).block()).isNull()
		assertThat(accessor.countChannelsInGuild(guildId).block()!!).isZero
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.channels.collectList().block())
			.noneMatch { it.id().asLong() == channelId }
	}

	@Test
	fun onGuildDelete_deleteEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addEmojis(emoji(emojiId).build()).build())
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiId).block()).isNotNull
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isOne
		assertThat(accessor.getGuildById(guildId).block()!!.emojis()).hasSize(1)
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block()).hasSize(1)
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiId }


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getEmojiById(guildId, emojiId).block()).isNull()
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isZero
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiId }
	}

	@Test
	fun onGuildDelete_deleteMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder().guild(guild(guildId).addMembers(member(userId).build()).build()).build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getMemberById(guildId, userId).block()).isNotNull
		assertThat(accessor.countMembersInGuild(guildId).block()!!).isOne
		assertThat(accessor.getGuildById(guildId).block()!!.members()).hasSize(1)
		assertThat(accessor.getMembersInGuild(guildId).collectList().block()).hasSize(1)
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getMemberById(guildId, userId).block()).isNull()
		assertThat(accessor.countMembersInGuild(guildId).block()!!).isZero
		assertThat(accessor.getMembersInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.members.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onGuildDelete_deleteMessages() {
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addChannels(channel(channelId).guildId(Possible.absent()).build())
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()

		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, authorId).build())
			.build()

		updater.onMessageCreate(0, messageCreate).block()

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.id().asLong() == messageId && it.author().id().asLong() == authorId }
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isOne

		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isZero
	}

	@Test
	fun onGuildDelete_deletePresences() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addPresences(presence(userId).build()).build()).build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getPresenceById(guildId, userId).block()).isNotNull
		assertThat(accessor.countPresencesInGuild(guildId).block()!!).isOne
		assertThat(accessor.getPresencesInGuild(guildId).collectList().block()).hasSize(1)
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getPresenceById(guildId, userId).block()).isNull()
		assertThat(accessor.countPresencesInGuild(guildId).block()!!).isZero
		assertThat(accessor.getPresencesInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.presences.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onGuildDelete_deleteRoles() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder().guild(guild(guildId).addRoles(role(roleId).build()).build()).build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getRoleById(guildId, roleId).block()).isNotNull
		assertThat(accessor.countRolesInGuild(guildId).block()!!).isOne
		assertThat(accessor.getGuildById(guildId).block()!!.roles()).hasSize(1)
		assertThat(accessor.getRolesInGuild(guildId).collectList().block()).hasSize(1)
		assertThat(accessor.roles.collectList().block())
			.anyMatch { it.id().asLong() == roleId }


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getRoleById(guildId, roleId).block()).isNull()
		assertThat(accessor.countRolesInGuild(guildId).block()!!).isZero
		assertThat(accessor.getRolesInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.roles.collectList().block())
			.noneMatch { it.id().asLong() == roleId }
	}

	@Test
	fun onGuildDelete_deleteVoiceStates() {
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
						voiceStateInChannel(guildId, channelIdA, userIdA).build(),
						voiceStateInChannel(guildId, channelIdB, userIdB).build(),
						voiceStateInChannel(guildId, channelIdB, userIdC).build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getVoiceStateById(guildId, userIdA).block()).isNotNull
		assertThat(accessor.getVoiceStateById(guildId, userIdB).block()).isNotNull
		assertThat(accessor.getVoiceStateById(guildId, userIdC).block()).isNotNull

		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!).isEqualTo(1)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!).isEqualTo(2)

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block()).hasSize(3)
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block()).hasSize(1)
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block()).hasSize(2)

		assertThat(accessor.voiceStates.collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))


		updater.onGuildDelete(0, guildDelete(guildId)).block()

		assertThat(accessor.getVoiceStateById(guildId, userIdA).block()).isNull()
		assertThat(accessor.getVoiceStateById(guildId, userIdB).block()).isNull()
		assertThat(accessor.getVoiceStateById(guildId, userIdC).block()).isNull()

		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!).isZero
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!).isZero
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!).isZero

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block()).isEmpty()
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block()).isEmpty()
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block()).isEmpty()

		assertThat(accessor.voiceStates.collectList().block())
			.noneMatch(isVoiceState(guildId, channelIdA, userIdA))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdB))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdC))
	}

	@Test
	fun onGuildUpdate_updateGuild() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.name("Deep Space 9")
					.build()
			)
			.build()

		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getGuildById(guildId).block())
			.matches { it.id().asLong() == guildId }
			.matches { it.name() == "Deep Space 9" }

		val guildUpdate = GuildUpdate.builder()
			.guild(guildUpdate(guildId).build())
			.build()


		updater.onGuildUpdate(0, guildUpdate).block()

		assertThat(accessor.getGuildById(guildId).block())
			.matches { it.id().asLong() == guildId }
			.matches { it.name() == "Terok Nor" }
	}

	// https://github.com/Discord4J/Discord4J/issues/429
	@Test
	fun afterChunkingLargeGuild_noDuplicateMembers() {
		val guildId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()

		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.large(true)
					.addMembers(
						member(userIdA).build(),
					)
					.build()
			)
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		val guildMembersChunk = GuildMembersChunk.builder()
			.guildId(guildId)
			.chunkIndex(0)
			.chunkCount(1)
			.addMembers(
				member(userIdA).build(),
				member(userIdB).build(),
			)
			.build()
		updater.onGuildMembersChunk(0, guildMembersChunk).block()


		assertThat(accessor.getMembersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.user().id().asLong() == userIdA }
			.anyMatch { it.user().id().asLong() == userIdB }
	}
}
