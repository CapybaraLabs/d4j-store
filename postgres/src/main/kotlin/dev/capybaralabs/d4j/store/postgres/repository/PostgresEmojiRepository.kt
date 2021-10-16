package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
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
internal class PostgresEmojiRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	EmojiRepository {

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
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(emoji), shardId).then()
	}

	override fun saveAll(guildId: Long, emojis: List<EmojiData>, shardId: Int): Mono<Void> {
		val guildEmojis = emojis.filter { it.id().isPresent }
		if (guildEmojis.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_emoji VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (emoji_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)

				for (guildEmoji in guildEmojis) {
					statement
						.bind("$1", guildEmoji.id().get().asLong())
						.bind("$2", guildId)
						.bind("$3", serde.serializeToString(guildEmoji))
						.bind("$4", shardId)
						.add()
				}
				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteByIds(emojiIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_emoji WHERE emoji_id = ANY($1) AND guild_id = $2")
					.bind("$1", emojiIds.toTypedArray())
					.bind("$2", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_emoji WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}


	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_emoji WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_emoji")
					.execute().mapToCount()
			}
		}
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_emoji WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_emoji")
					.execute().deserializeManyFromData(EmojiData::class.java, serde)
			}
		}
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_emoji WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(EmojiData::class.java, serde)
			}
		}
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
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
