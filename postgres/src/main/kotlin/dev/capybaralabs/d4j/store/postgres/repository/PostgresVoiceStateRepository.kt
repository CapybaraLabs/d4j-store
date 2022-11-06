package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
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
internal class PostgresVoiceStateRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	VoiceStateRepository {

	init {
		withConnectionMany(factory, "PostgresVoiceStateRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_voice_state (
					user_id BIGINT NOT NULL,
					channel_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_voice_state_pkey PRIMARY KEY (user_id, channel_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(voiceState: VoiceStateData, shardId: Int): Mono<Void> {
		return saveAll(listOf(voiceState), shardId).then()
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardId: Int): Mono<Void> {
		val voiceStatesInChannels = voiceStates.filter { it.channelId().isPresent && it.guildId().isPresent() }
		if (voiceStatesInChannels.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.saveAll", voiceStatesInChannels.size) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_voice_state VALUES ($1, $2, $3, $4, $5)
						ON CONFLICT (user_id, channel_id) DO UPDATE SET guild_id = $3, data = $4, shard_index = $5
					""".trimIndent()
				)

				for (voiceState in voiceStatesInChannels) {
					statement.add()
						.bind("$1", voiceState.userId().asLong())
						.bind("$2", voiceState.channelId().orElseThrow().asLong())
						.bind("$3", voiceState.guildId().get().asLong())
						.bind("$4", serde.serialize(voiceState))
						.bind("$5", shardId)
				}

				statement.executeConsumingAll().then()
			}
		}

		// TODO consider deleting those that are not in a channel?
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.deleteById") {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE user_id = $1 AND guild_id = $2")
					.bind("$1", userId)
					.bind("$2", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.deleteByGuildId") {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_voice_state WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countVoiceStates(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.countVoiceStates") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state")
					.execute().mapToCount()
			}
		}
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.countVoiceStatesInGuild") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.countVoiceStatesInChannel") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_voice_state WHERE guild_id = $1 AND channel_id = $2")
					.bind("$1", guildId)
					.bind("$2", channelId)
					.execute().mapToCount()
			}
		}
	}

	override fun getVoiceStates(): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresVoiceStateRepository.getVoiceStates") {
				it.createStatement("SELECT data FROM d4j_discord_voice_state")
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresVoiceStateRepository.getVoiceStatesInChannel") {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1 AND channel_id = $2")
					.bind("$1", guildId)
					.bind("$2", channelId)
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresVoiceStateRepository.getVoiceStatesInGuild") {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(VoiceStateData::class.java, serde)
			}
		}
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return Mono.defer {
			withConnection(factory, "PostgresVoiceStateRepository.getVoiceStateById") {
				it.createStatement("SELECT data FROM d4j_discord_voice_state WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(VoiceStateData::class.java, serde)
			}
		}
	}
}
