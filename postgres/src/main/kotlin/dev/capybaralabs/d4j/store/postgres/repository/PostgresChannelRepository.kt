package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsuming
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.ChannelData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the channel table
 */
internal class PostgresChannelRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	ChannelRepository {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_channel (
					channel_id BIGINT NOT NULL,
					guild_id BIGINT,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_channel_pkey PRIMARY KEY (channel_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	override fun save(channel: ChannelData, shardIndex: Int): Mono<Void> {
		return Mono.defer {
			saveAll(listOf(channel), shardIndex).then()
		}
	}

	override fun saveAll(channels: List<ChannelData>, shardIndex: Int): Flux<Int> {
		if (channels.isEmpty()) {
			return Flux.empty()
		}
		return Flux.defer {
			withConnectionMany(factory) { connection ->
				var statement = connection.createStatement(
					"""
					INSERT INTO d4j_discord_channel VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (channel_id) DO UPDATE SET guild_id = $2, data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)

				for (channel in channels) {
					val guildId = channel.guildId().toOptional()

					statement.bind("$1", channel.id().asLong())
					statement = if (guildId.isPresent) {
						statement.bind("$2", guildId.get().asLong())
					} else {
						statement.bindNull("$2", java.lang.Long::class.java)
					}
					statement
						.bind("$3", serde.serializeToString(channel))
						.bind("$4", shardIndex)

					statement.add()
				}

				statement.executeConsuming()
			}
		}
	}

	override fun delete(channelId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE channel_id = $1")
					.bind("$1", channelId)
					.executeConsumingSingle()
			}
		}
	}

	override fun deleteByIds(channelIds: List<Long>): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE channel_id = ANY($1)")
					.bind("$1", channelIds.toTypedArray())
					.executeConsumingSingle()
			}
		}
	}


	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_channel")
					.execute().mapToCount()
			}
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_channel WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_channel WHERE channel_id = $1")
					.bind("$1", channelId)
					.execute().deserializeOneFromData(ChannelData::class.java, serde)
			}
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_channel")
					.execute().deserializeManyFromData(ChannelData::class.java, serde)
			}
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_channel WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(ChannelData::class.java, serde)
			}
		}
	}
}
