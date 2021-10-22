package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.common.toLong
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
internal class PostgresPresenceRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) : PresenceRepository {

	init {
		withConnectionMany(factory, "PostgresPresenceRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_presence (
					user_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_presence_pkey PRIMARY KEY (guild_id, user_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void> {
		return saveAll(mapOf(Pair(guildId, listOf(presence))), shardId).then()
	}

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	override fun saveAll(presencesByGuild: Map<Long, List<PresenceData>>, shardId: Int): Mono<Void> {
		val filtered = presencesByGuild.filter { it.value.isNotEmpty() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.saveAll") {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_presence VALUES ($1, $2, $3, $4)
						ON CONFLICT (guild_id, user_id) DO UPDATE SET data = $3, shard_index = $4
					""".trimIndent()
				)
				for (guildPresences in filtered) {
					val guildId = guildPresences.key
					for (presence in guildPresences.value) {
						statement
							.bind("$1", presence.user().id().asLong())
							.bind("$2", guildId)
							.bind("$3", serde.serialize(presence))
							.bind("$4", shardId)
							.add()
					}
				}
				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.deleteById") {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.deleteByGuildId") {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_presence WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countPresences(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.countPresences") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_presence")
					.execute().mapToCount()
			}
		}
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.countPresencesInGuild") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getPresences(): Flux<PresenceData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresPresenceRepository.getPresences") {
				it.createStatement("SELECT data FROM d4j_discord_presence")
					.execute().deserializeManyFromData(PresenceData::class.java, serde)
			}
		}
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresPresenceRepository.getPresencesInGuild") {
				it.createStatement("SELECT data FROM d4j_discord_presence WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(PresenceData::class.java, serde)
			}
		}
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return Mono.defer {
			withConnection(factory, "PostgresPresenceRepository.getPresenceById") {
				it.createStatement("SELECT data FROM d4j_discord_presence WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(PresenceData::class.java, serde)
			}
		}
	}
}
