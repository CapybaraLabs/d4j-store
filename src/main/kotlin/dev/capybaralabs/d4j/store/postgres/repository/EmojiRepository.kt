package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsuming
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.EmojiData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the emoji table
 */
internal class EmojiRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_emoji (
				    emoji_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_emoji_pkey PRIMARY KEY (emoji_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	fun save(guildId: Long, emoji: EmojiData, shardIndex: Int): Mono<Void> {
		val id = emoji.id()
		if (id.isEmpty) {
			// TODO is this ok?
			return Mono.empty()
		}
		return Mono.defer {
			withConnection(factory) {
				it.createStatement(
					"""
					INSERT INTO d4j_discord_emoji VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (emoji_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)
					.bind("$1", id.get().asLong())
					.bind("$2", guildId)
					.bind("$3", serde.serializeToString(emoji))
					.bind("$4", shardIndex)
					.executeConsumingSingle().then()
			}
		}
	}

	fun deleteByIds(emojiIds: List<Long>): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_emoji WHERE emoji_id = ANY($1)")
					.bind("$1", emojiIds.toTypedArray())
					.executeConsumingSingle()
			}
		}
	}


	fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_emoji WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	fun countEmojis(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_emoji")
					.execute().mapToCount()
			}
		}
	}

	fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_emoji WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_emoji")
					.execute().deserializeManyFromData(EmojiData::class.java, serde)
			}
		}
	}

	fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_emoji WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(EmojiData::class.java, serde)
			}
		}
	}

	fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_emoji WHERE guild_id = $1 AND emoji_id = $2")
					.bind("$1", guildId)
					.bind("$2", emojiId)
					.execute().deserializeOneFromData(EmojiData::class.java, serde)
			}
		}
	}

}
