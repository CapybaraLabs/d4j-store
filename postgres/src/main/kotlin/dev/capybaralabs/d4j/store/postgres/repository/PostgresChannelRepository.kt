package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
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
		withConnectionMany(factory, "PostgresChannelRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_channel (
					channel_id BIGINT NOT NULL,
					guild_id BIGINT,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_channel_pkey PRIMARY KEY (channel_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(channel: ChannelData, shardId: Int): Mono<Void> {
		return Mono.defer {
			saveAll(listOf(channel), shardId)
		}
	}

	override fun saveAll(channels: List<ChannelData>, shardId: Int): Mono<Void> {
		if (channels.isEmpty()) {
			return Mono.empty()
		}
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.saveAll", channels.size) { connection ->
				var statement = connection.createStatement(
					"""
					INSERT INTO d4j_discord_channel VALUES ($1, $2, $3, $4)
						ON CONFLICT (channel_id) DO UPDATE SET guild_id = $2, data = $3, shard_index = $4
					""".trimIndent()
				)

				for (channel in channels) {
					val guildId = channel.guildId().toOptional()

					statement.add().bind("$1", channel.id().asLong())
					statement = if (guildId.isPresent) {
						statement.bind("$2", guildId.get().asLong())
					} else {
						statement.bindNull("$2", java.lang.Long::class.java)
					}
					statement
						.bind("$3", serde.serialize(channel))
						.bind("$4", shardId)
				}

				statement.executeConsumingAll().then()
			}
		}
	}

	override fun delete(channelId: Long, guildId: Long?): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.delete") {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE channel_id = $1")
					.bind("$1", channelId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.deleteByGuildId") {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}


	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_channel WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.countChannels") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_channel")
					.execute().mapToCount()
			}
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.countChannelsInGuild") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_channel WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			withConnection(factory, "PostgresChannelRepository.getChannelById") {
				it.createStatement("SELECT data FROM d4j_discord_channel WHERE channel_id = $1")
					.bind("$1", channelId)
					.execute().deserializeOneFromData(ChannelData::class.java, serde)
			}
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresChannelRepository.getChannels") {
				it.createStatement("SELECT data FROM d4j_discord_channel")
					.execute().deserializeManyFromData(ChannelData::class.java, serde)
			}
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresChannelRepository.getChannelsInGuild") {
				it.createStatement("SELECT data FROM d4j_discord_channel WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(ChannelData::class.java, serde)
			}
		}
	}
}
