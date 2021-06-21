package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.ChannelData
import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.GuildCreateData
import discord4j.discordjson.json.ImmutableChannelData
import discord4j.discordjson.json.ImmutableEmojiData
import discord4j.discordjson.json.ImmutableGuildCreateData
import discord4j.discordjson.json.ImmutableMessageData
import discord4j.discordjson.json.ImmutableUserData
import discord4j.discordjson.json.MessageData
import discord4j.discordjson.json.UserData
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong


private val LONGS = AtomicLong(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE / 2, Long.MAX_VALUE))
internal fun generateUniqueSnowflakeId(): Long {
    return LONGS.decrementAndGet()
}

internal val storeLayout = PostgresStoreLayout(
    ConnectionPool(
        ConnectionPoolConfiguration
            .builder(ConnectionFactories.get("r2dbc:tc:postgresql:///test?TC_IMAGE_TAG=13"))
            .build()
    )
)
internal val accessor = storeLayout.dataAccessor
internal val updater = storeLayout.gatewayDataUpdater


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

internal fun user(userId: Long): ImmutableUserData.Builder {
    return UserData.builder()
        .username("Q")
        .discriminator("6969")
        .id(userId)
}
