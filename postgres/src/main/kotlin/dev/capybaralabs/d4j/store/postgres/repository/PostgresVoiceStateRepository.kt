package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsuming
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.VoiceStateData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the role table
 */
internal class PostgresVoiceStateRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_voice_state (
					user_id BIGINT NOT NULL,
					channel_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_voice_state_pkey PRIMARY KEY (user_id, channel_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	fun save(voiceState: VoiceStateData, shardIndex: Int): Mono<Void> {
		return saveAll(listOf(voiceState), shardIndex).then()
	}

	fun saveAll(voiceStates: List<VoiceStateData>, shardIndex: Int): Flux<Int> {
		if (voiceStates.isEmpty()) {
			return Flux.empty()
		}

		val voiceStatesInChannels = voiceStates.filter { it.channelId().isPresent }

		return Flux.defer {
			withConnectionMany(factory) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_voice_state VALUES ($1, $2, $3, $4::jsonb, $5)
						ON CONFLICT (user_id, channel_id) DO UPDATE SET guild_id = $3, data = $4::jsonb, shard_index = $5
					""".trimIndent()
				)

				for (voiceState in voiceStatesInChannels) {
					statement
						.bind("$1", voiceState.userId().asLong())
						.bind("$2", voiceState.channelId().get().asLong())
						.bind("$3", voiceState.guildId().get().asLong()) // TODO check if present or pass as param
						.bind("$4", serde.serializeToString(voiceState))
						.bind("$5", shardIndex)
						.add()
				}

				statement.executeConsuming()
			}
		}

		// TODO consider deleting those that are not in a channel?
	}

	fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE user_id = $1 AND guild_id = $2")
					.bind("$1", userId)
					.bind("$2", guildId)
					.executeConsumingSingle()
			}
		}
	}

	fun deleteByGuildId(guildId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle()
			}
		}
	}

	fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	fun countVoiceStates(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state")
					.execute().mapToCount()
			}
		}
	}

	fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state WHERE guild_id = $1 AND channel_id = $2")
					.bind("$1", guildId)
					.bind("$2", channelId)
					.execute().mapToCount()
			}
		}
	}

	fun getVoiceStates(): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_voice_state")
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1 AND channel_id = $2")
					.bind("$1", guildId)
					.bind("$2", channelId)
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(VoiceStateData::class.java, serde)
			}
		}
	}
}
