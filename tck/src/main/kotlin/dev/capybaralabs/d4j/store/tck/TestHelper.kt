package dev.capybaralabs.d4j.store.tck

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
import discord4j.discordjson.json.ImmutableStickerData
import discord4j.discordjson.json.ImmutableUserData
import discord4j.discordjson.json.ImmutableVoiceStateData
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.MessageData
import discord4j.discordjson.json.PartialMessageData
import discord4j.discordjson.json.PartialUserData
import discord4j.discordjson.json.PresenceData
import discord4j.discordjson.json.RoleData
import discord4j.discordjson.json.StickerData
import discord4j.discordjson.json.UnavailableGuildData
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.VoiceStateData
import discord4j.discordjson.json.gateway.GuildDelete
import discord4j.discordjson.possible.Possible
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.ListAssert


private val LONGS = AtomicLong(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE / 2, Long.MAX_VALUE))
internal fun generateUniqueSnowflakeId(): Long {
	return LONGS.decrementAndGet()
}

// TODO add & verify all optional parameters

// TODO use random alphanumerics or similar
// TODO extract common assertions from tests at the end
// TODO build library of shorthands to create/trigger events
// TODO test retuning previous versions

internal fun channel(channelId: Long): ImmutableChannelData.Builder {
	return ChannelData.builder()
		.id(channelId)
		.type(2)
}

internal fun emoji(emojiId: Long): ImmutableEmojiData.Builder {
	return EmojiData.builder()
		.id(emojiId)
}

internal fun unicodeEmoji(name: String): ImmutableEmojiData.Builder {
	return EmojiData.builder()
		.name(name)
}

internal fun guild(guildId: Long): ImmutableGuildCreateData.Builder {
	return GuildCreateData.builder()
		.id(guildId)
		.name("Deep Space 9")
		.ownerId(generateUniqueSnowflakeId())
		.verificationLevel(42)
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

internal fun sticker(stickerId: Long): ImmutableStickerData.Builder {
	return StickerData.builder()
		.id(stickerId)
		.name(stickerId.toString())
		.type(2)
		.formatType(3)
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


internal fun voiceStateInChannel(guildId: Long, channelId: Long, userId: Long): ImmutableVoiceStateData.Builder {
	return voiceStateNoChannel(guildId, userId)
		.channelId(channelId)
		.sessionId("$guildId:$channelId:$userId")
}

internal fun voiceStateNoChannel(guildId: Long, userId: Long): ImmutableVoiceStateData.Builder {
	return VoiceStateData.builder()
		.guildId(guildId)
		.userId(userId)
		.sessionId("")
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

/**
 * Put type information back into extracted value
 */
internal fun <T, ACTUAL> AbstractObjectAssert<*, ACTUAL>.extractingList(extractor: (ACTUAL) -> List<T>): ListAssert<T> {
	return extracting(extractor)
		.asList()
		.let {
			@Suppress("UNCHECKED_CAST")
			it as ListAssert<T>
		}
}
