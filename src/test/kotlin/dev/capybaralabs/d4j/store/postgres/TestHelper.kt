package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.ActivityData
import discord4j.discordjson.json.ChannelData
import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.GuildCreateData
import discord4j.discordjson.json.GuildUpdateData
import discord4j.discordjson.json.ImmutableChannelData
import discord4j.discordjson.json.ImmutableEmojiData
import discord4j.discordjson.json.ImmutableGuildCreateData
import discord4j.discordjson.json.ImmutableGuildUpdateData
import discord4j.discordjson.json.ImmutableMemberData
import discord4j.discordjson.json.ImmutableMessageData
import discord4j.discordjson.json.ImmutablePartialMessageData
import discord4j.discordjson.json.ImmutablePartialUserData
import discord4j.discordjson.json.ImmutablePresenceData
import discord4j.discordjson.json.ImmutableRoleData
import discord4j.discordjson.json.ImmutableUserData
import discord4j.discordjson.json.ImmutableVoiceStateData
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.MessageData
import discord4j.discordjson.json.PartialMessageData
import discord4j.discordjson.json.PartialUserData
import discord4j.discordjson.json.PresenceData
import discord4j.discordjson.json.RoleData
import discord4j.discordjson.json.UnavailableGuildData
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.VoiceStateData
import discord4j.discordjson.json.gateway.GuildDelete
import discord4j.discordjson.possible.Possible
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import reactor.tools.agent.ReactorDebugAgent


private val LONGS = AtomicLong(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE / 2, Long.MAX_VALUE))
internal fun generateUniqueSnowflakeId(): Long {
	return LONGS.decrementAndGet()
}

internal val storeLayout = PostgresStoreLayout(connectionPool())
private fun connectionPool(): ConnectionPool {
	ReactorDebugAgent.init()
	ReactorDebugAgent.processExistingClasses()
	return ConnectionPool(
		ConnectionPoolConfiguration
			.builder(ConnectionFactories.get("r2dbc:tc:postgresql:///test?TC_IMAGE_TAG=13"))
			.build()
	)
}

internal val accessor = storeLayout.dataAccessor
internal val updater = storeLayout.gatewayDataUpdater

// TODO add & verify all optional parameters

internal fun channel(channelId: Long): ImmutableChannelData.Builder {
	return ChannelData.builder()
		.id(channelId)
		.type(2)
}

internal fun emoji(emojiId: Long): ImmutableEmojiData.Builder {
	return EmojiData.builder()
		.id(emojiId)
}

internal fun guild(guildId: Long): ImmutableGuildCreateData.Builder {
	return GuildCreateData.builder()
		.id(guildId)
		.name("Deep Space 9")
		.ownerId(generateUniqueSnowflakeId())
		.verificationLevel(42)
		.region("Alpha Quadrant")
		.afkTimeout(42)
		.defaultMessageNotifications(42)
		.explicitContentFilter(42)
		.mfaLevel(42)
		.premiumTier(42)
		.preferredLocale("Klingon")
		.joinedAt(Instant.now().toString())
		.large(false)
		.memberCount(42)
		.nsfwLevel(69)
}

internal fun guildUpdate(guildId: Long): ImmutableGuildUpdateData.Builder {
	return GuildUpdateData.builder()
		.id(guildId)
		.name("Terok Nor")
		.ownerId(generateUniqueSnowflakeId())
		.verificationLevel(42)
		.region("Alpha Quadrant")
		.afkTimeout(42)
		.defaultMessageNotifications(42)
		.explicitContentFilter(42)
		.mfaLevel(42)
		.premiumTier(42)
		.preferredLocale("Cardassian")
		.nsfwLevel(69)
}


internal fun member(userId: Long): ImmutableMemberData.Builder {
	return MemberData.builder()
		.user(user(userId).build())
		.deaf(false)
		.mute(false)
}

internal fun message(channelId: Long, messageId: Long, authorId: Long): ImmutableMessageData.Builder {
	return MessageData.builder()
		.id(messageId)
		.channelId(channelId)
		.author(user(authorId).build())
		.timestamp("42")
		.tts(false)
		.mentionEveryone(false)
		.pinned(false)
		.type(2)
		.content("🖖")
}

internal fun partialMessage(channelId: Long, messageId: Long): ImmutablePartialMessageData.Builder {
	return PartialMessageData.builder()
		.id(messageId)
		.channelId(channelId)
}

internal fun presence(userId: Long): ImmutablePresenceData.Builder {
	return PresenceData.builder()
		.user(partialUser(userId).build())
		.status("online")
		.clientStatus(
			ClientStatusData.builder()
				.desktop("idle")
				.mobile("online")
				.web(Possible.absent())
				.build()
		)
		.addActivity(
			ActivityData.builder()
				.id(userId.toString())
				.name("Tongo")
				.type(0)
				.createdAt(Instant.now().toEpochMilli())
				.build()
		)
}

internal fun role(roleId: Long): ImmutableRoleData.Builder {
	return RoleData.builder()
		.id(roleId)
		.name("Ensign")
		.color(0xFFFFFF)
		.hoist(false)
		.permissions(8)
		.mentionable(true)
		.position(Short.MAX_VALUE.toInt())
		.managed(false)
}

internal fun user(userId: Long): ImmutableUserData.Builder {
	return UserData.builder()
		.id(userId)
		.username("Q")
		.discriminator("6969")
}

internal fun partialUser(userId: Long): ImmutablePartialUserData.Builder {
	return PartialUserData.builder()
		.id(userId)
		.username("Q")
		.discriminator("6969")
}


internal fun voiceState(guildId: Long, channelId: Long, userId: Long): ImmutableVoiceStateData.Builder {
	return VoiceStateData.builder()
		.guildId(guildId)
		.channelId(channelId)
		.userId(userId)
		.sessionId("$guildId:$channelId:$userId")
		.deaf(false)
		.mute(true)
		.selfDeaf(false)
		.selfMute(true)
		.selfVideo(false)
		.suppress(true)
}


internal fun isVoiceState(guildId: Long, channelId: Long, userId: Long): (VoiceStateData) -> Boolean {
	return {
		it.guildId().get().asLong() == guildId
			&& it.channelId().get().asLong() == channelId
			&& it.userId().asLong() == userId
	}
}

internal fun guildDelete(guildId: Long): GuildDelete {
	return GuildDelete.builder()
		.guild(UnavailableGuildData.builder().id(guildId).build())
		.build()
}

// TODO onShardInvalidation
