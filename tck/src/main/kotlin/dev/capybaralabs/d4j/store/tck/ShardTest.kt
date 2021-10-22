package dev.capybaralabs.d4j.store.tck

import discord4j.common.store.api.`object`.InvalidationCause
import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildRoleCreate
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.json.gateway.PresenceUpdate
import discord4j.discordjson.json.gateway.VoiceStateUpdateDispatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Make sure the shardIds used here are isolated from the rest of the test suite, so we don't delete their data accidentally
 */
internal class ShardTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun onShardInvalidation_deleteChannelsOnThisShard() {
		val channelId = generateUniqueSnowflakeId()
		createChannel(100, channelId)

		updater.onShardInvalidation(100, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getChannelById(channelId).block()).isNull()
		assertThat(accessor.channels.collectList().block())
			.noneMatch { it.id().asLong() == channelId }
	}

	@Test
	fun onShardInvalidation_keepChannelsOnOtherShards() {
		val channelId = generateUniqueSnowflakeId()
		createChannel(111, channelId)

		updater.onShardInvalidation(110, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getChannelById(channelId).block())
			.matches { it.id().asLong() == channelId }
		assertThat(accessor.channels.collectList().block())
			.anyMatch { it.id().asLong() == channelId }
	}

	@Test
	fun onShardInvalidation_deleteEmojisOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addEmojis(emoji(emojiId).build()).build())
			.build()
		updater.onGuildCreate(200, guildCreate).block()

		updater.onShardInvalidation(200, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getEmojiById(guildId, emojiId).block()).isNull()
		assertThat(accessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiId }
	}

	@Test
	fun onShardInvalidation_keepEmojisOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addEmojis(emoji(emojiId).build()).build())
			.build()
		updater.onGuildCreate(211, guildCreate).block()

		updater.onShardInvalidation(210, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getEmojiById(guildId, emojiId).block())
			.matches { it.id().get().asLong() == emojiId }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiId }
	}


	@Test
	fun onShardInvalidation_deleteGuildsOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()
		updater.onGuildCreate(300, guildCreate).block()

		updater.onShardInvalidation(300, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getGuildById(guildId).block()).isNull()
		assertThat(accessor.guilds.collectList().block())
			.noneMatch { it.id().asLong() == guildId }
	}

	@Test
	fun onShardInvalidation_keepGuildsOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()
		updater.onGuildCreate(311, guildCreate).block()

		updater.onShardInvalidation(310, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getGuildById(guildId).block())
			.matches { it.id().asLong() == guildId }
		assertThat(accessor.guilds.collectList().block())
			.anyMatch { it.id().asLong() == guildId }
	}


	@Test
	fun onShardInvalidation_deleteMembersOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addMembers(member(userId).build()).build())
			.build()
		updater.onGuildCreate(400, guildCreate).block()

		updater.onShardInvalidation(400, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getMemberById(guildId, userId).block()).isNull()
		assertThat(accessor.members.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onShardInvalidation_keepMembersOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).addMembers(member(userId).build()).build())
			.build()
		updater.onGuildCreate(411, guildCreate).block()

		updater.onShardInvalidation(410, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getMemberById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId }
		assertThat(accessor.members.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }
	}


	@Test
	fun onShardInvalidation_deleteMessagesOnThisShard() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, authorId).build())
			.build()
		updater.onMessageCreate(500, messageCreate).block()

		updater.onShardInvalidation(500, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
		assertThat(accessor.messages.collectList().block())
			.noneMatch { it.id().asLong() == messageId }
	}

	@Test
	fun onShardInvalidation_keepMessagesOnOtherShards() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, authorId).build())
			.build()
		updater.onMessageCreate(510, messageCreate).block()

		updater.onShardInvalidation(511, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.id().asLong() == messageId }
		assertThat(accessor.messages.collectList().block())
			.anyMatch { it.id().asLong() == messageId }
	}


	@Test
	fun onShardInvalidation_deletePresencesOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(600, presenceUpdate).block()

		updater.onShardInvalidation(600, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getPresenceById(guildId, userId).block()).isNull()
		assertThat(accessor.presences.collectList().block())
			.noneMatch { it.user().id().asLong() == userId }
	}

	@Test
	fun onShardInvalidation_keepPresencesOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val presenceUpdate = PresenceUpdate.builder()
			.guildId(guildId)
			.user(partialUser(userId).build())
			.status("online")
			.clientStatus(ClientStatusData.builder().build())
			.build()
		updater.onPresenceUpdate(610, presenceUpdate).block()

		updater.onShardInvalidation(611, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getPresenceById(guildId, userId).block())
			.matches { it.user().id().asLong() == userId }
		assertThat(accessor.presences.collectList().block())
			.anyMatch { it.user().id().asLong() == userId }
	}


	@Test
	fun onShardInvalidation_deleteRolesOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()
		val guildRoleCreate = GuildRoleCreate.builder()
			.guildId(guildId)
			.role(role(roleId).build())
			.build()
		updater.onGuildRoleCreate(700, guildRoleCreate).block()

		updater.onShardInvalidation(700, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getRoleById(guildId, roleId).block()).isNull()
		assertThat(accessor.roles.collectList().block())
			.noneMatch { it.id().asLong() == roleId }
	}

	@Test
	fun onShardInvalidation_keepRolesOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val roleId = generateUniqueSnowflakeId()
		val guildRoleCreate = GuildRoleCreate.builder()
			.guildId(guildId)
			.role(role(roleId).build())
			.build()
		updater.onGuildRoleCreate(710, guildRoleCreate).block()

		updater.onShardInvalidation(711, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getRoleById(guildId, roleId).block())
			.matches { it.id().asLong() == roleId }
		assertThat(accessor.roles.collectList().block())
			.anyMatch { it.id().asLong() == roleId }
	}


	@Test
	fun onShardInvalidation_deleteVoiceStatesOnThisShard() {
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val voiceStateUpdate = VoiceStateUpdateDispatch.builder()
			.voiceState(voiceStateInChannel(guildId, channelId, userId).build())
			.build()
		updater.onVoiceStateUpdateDispatch(800, voiceStateUpdate).block()

		updater.onShardInvalidation(800, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getVoiceStateById(guildId, userId).block()).isNull()
		assertThat(accessor.voiceStates.collectList().block())
			.noneMatch(isVoiceState(guildId, channelId, userId))
	}

	@Test
	fun onShardInvalidation_keepVoiceStatesOnOtherShards() {
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val voiceStateUpdate = VoiceStateUpdateDispatch.builder()
			.voiceState(voiceStateInChannel(guildId, channelId, userId).build())
			.build()
		updater.onVoiceStateUpdateDispatch(810, voiceStateUpdate).block()

		updater.onShardInvalidation(811, InvalidationCause.HARD_RECONNECT).block()

		assertThat(accessor.getVoiceStateById(guildId, userId).block())
			.matches(isVoiceState(guildId, channelId, userId))
		assertThat(accessor.voiceStates.collectList().block())
			.anyMatch(isVoiceState(guildId, channelId, userId))
	}

	@Test
	fun onShardInvalidation_deleteOrphanedUsers() {
		val guildIdA = generateUniqueSnowflakeId()
		val guildIdB = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val guildCreateA = GuildCreate.builder()
			.guild(
				guild(guildIdA).addMembers(
					member(userIdA).build(),
					member(userIdB).build(),
				).build()
			).build()
		updater.onGuildCreate(900, guildCreateA).block()
		val guildCreateB = GuildCreate.builder()
			.guild(
				guild(guildIdB).addMembers(
					member(userIdB).build(),
				).build()
			).build()
		updater.onGuildCreate(901, guildCreateB).block()

		assertThat(accessor.getUserById(userIdA).block()).isNotNull
		assertThat(accessor.getUserById(userIdB).block()).isNotNull

		updater.onShardInvalidation(900, InvalidationCause.LOGOUT).block()

		assertThat(accessor.getUserById(userIdA).block()).isNull()
		assertThat(accessor.getUserById(userIdB).block()).isNotNull
	}

	private fun createChannel(shardId: Int, channelId: Long) {
		val channelCreate = ChannelCreate.builder()
			.channel(channel(channelId).build())
			.build()

		updater.onChannelCreate(shardId, channelCreate).block()
	}
}
