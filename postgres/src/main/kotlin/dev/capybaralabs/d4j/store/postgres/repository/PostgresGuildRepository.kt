package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.GuildData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the guild table
 */
internal class PostgresGuildRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	GuildRepository {

	init {
		withConnectionMany(factory, "PostgresGuildRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_guild (
					guild_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_guild_pkey PRIMARY KEY (guild_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guild: GuildData, shardId: Int): Mono<Void> {
		return saveAll(listOf(guild), shardId).then()
	}

	override fun saveAll(guilds: List<GuildData>, shardId: Int): Mono<Void> {
		if (guilds.isEmpty()) {
			return Mono.empty()
		}
		return Mono.defer {
			withConnection(factory, "PostgresGuildRepository.saveAll", guilds.size) { connection ->
				val statement = connection.createStatement(
					"""
					INSERT INTO d4j_discord_guild VALUES ($1, $2, $3)
						ON CONFLICT (guild_id) DO UPDATE SET data = $2, shard_index = $3
					""".trimIndent()
				)

				for (guild in guilds) {
					statement.add()
						.bind("$1", guild.id().asLong())
						.bind("$2", serde.serialize(guild))
						.bind("$3", shardId)
				}
				statement.executeConsumingAll().then()
			}
		}
	}


	override fun delete(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresGuildRepository.delete") {
				it.createStatement("DELETE FROM d4j_discord_guild WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresGuildRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_guild WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countGuilds(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresGuildRepository.countGuilds") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_guild")
					.execute().mapToCount()
			}
		}
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.defer {
			withConnection(factory, "PostgresGuildRepository.getGuilyById") {
				it.createStatement("SELECT data FROM d4j_discord_guild WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeOneFromData(GuildData::class.java, serde)
			}
		}
	}

	override fun getGuilds(): Flux<GuildData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresGuildRepository.getGuilds") {
				it.createStatement("SELECT data FROM d4j_discord_guild")
					.execute().deserializeManyFromData(GuildData::class.java, serde)
			}
		}
	}
}
