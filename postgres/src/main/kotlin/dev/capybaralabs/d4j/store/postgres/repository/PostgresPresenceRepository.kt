package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.PresenceData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the presence table
 */
internal class PostgresPresenceRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	PresenceRepository {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_presence (
					user_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_presence_pkey PRIMARY KEY (guild_id, user_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, presence: PresenceData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(presence), shardIndex).then()
	}

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	override fun saveAll(guildId: Long, presences: List<PresenceData>, shardIndex: Int): Mono<Void> {
		if (presences.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_presence VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (guild_id, user_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)
				for (presence in presences) {
					statement
						.bind("$1", presence.user().id().asLong())
						.bind("$2", guildId)
						.bind("$3", serde.serializeToString(presence))
						.bind("$4", shardIndex)
						.add()
				}

				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.executeConsumingSingle()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle()
			}
		}
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	override fun countPresences(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_presence")
					.execute().mapToCount()
			}
		}
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_presence")
					.execute().deserializeManyFromData(PresenceData::class.java, serde)
			}
		}
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(PresenceData::class.java, serde)
			}
		}
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_presence WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(PresenceData::class.java, serde)
			}
		}
	}
}
