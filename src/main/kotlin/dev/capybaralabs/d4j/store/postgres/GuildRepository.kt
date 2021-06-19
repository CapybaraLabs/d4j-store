package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.GuildData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the guild table
 */
internal class GuildRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_guild (
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_guild_pkey PRIMARY KEY (guild_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	fun save(guild: GuildData, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement(
					"""
					INSERT INTO d4j_discord_guild VALUES ($1, $2::jsonb, $3)
						ON CONFLICT (guild_id) DO UPDATE SET data = $2::jsonb, shard_index = $3
					""".trimIndent()
				)
					.bind("$1", guild.id().asLong())
					.bind("$2", serde.serializeToString(guild))
					.bind("$3", shardIndex)
					.executeConsumingSingle().then()
			}
		}
	}


	fun delete(guildId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_guild WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle()
			}
		}
	}

	fun removeChannel(channelId: Long, guildId: Long): Mono<Int> {
		// TODO: broken in postgres, cannot remove numeric entries from array. might need more complicated sql logic, or some other crap like fetching, editing, saving
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("UPDATE d4j_discord_guild SET data = jsonb_set(data, '{channels}', (data -> 'channels') - $1::TEXT) WHERE guild_id = $2")
					.bind("$1", channelId)
					.bind("$2", guildId)
					.executeConsumingSingle()
			}
		}
	}

	fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_guild WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	fun countGuilds(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_guild")
					.execute().mapToCount()
			}
		}
	}

	fun getGuildById(guildId: Long): Mono<GuildData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_guild WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeOneFromData(GuildData::class.java, serde)
			}
		}
	}

	fun getGuilds(): Flux<GuildData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_guild")
					.execute().deserializeManyFromData(GuildData::class.java, serde)
			}
		}
	}
}
