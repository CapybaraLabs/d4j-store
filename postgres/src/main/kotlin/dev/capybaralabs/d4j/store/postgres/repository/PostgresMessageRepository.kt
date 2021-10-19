package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.MessageData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the message table
 */
internal class PostgresMessageRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	MessageRepository {

	init {
		withConnectionMany(factory, "PostgresMessageRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_message (
				    message_id BIGINT NOT NULL,
					channel_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_message_pkey PRIMARY KEY (message_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(message: MessageData, shardId: Int): Mono<Void> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.save") {
				it.createStatement(
					"""
					INSERT INTO d4j_discord_message VALUES ($1, $2, $3, $4)
						ON CONFLICT (message_id) DO UPDATE SET data = $3, shard_index = $4
					""".trimIndent()
				)
					.bind("$1", message.id().asLong())
					.bind("$2", message.channelId().asLong())
					.bind("$3", serde.serialize(message))
					.bind("$4", shardId)
					.executeConsumingSingle().then()
			}
		}
	}

	override fun delete(messageId: Long, channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.delete") {
				it.createStatement("DELETE FROM d4j_discord_message WHERE message_id = $1")
					.bind("$1", messageId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByIds(messageIds: List<Long>, channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.deleteByIds") {
				it.createStatement("DELETE FROM d4j_discord_message WHERE message_id = ANY($1)")
					.bind("$1", messageIds.toTypedArray())
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_message WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByChannelId(channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.deleteByChannelId") {
				it.createStatement("DELETE FROM d4j_discord_message WHERE channel_id = $1")
					.bind("$1", channelId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.deleteByChannelIds") {
				it.createStatement("DELETE FROM d4j_discord_message WHERE channel_id = ANY($1)")
					.bind("$1", channelIds.toTypedArray())
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countMessages(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.countMessages") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_message")
					.execute().mapToCount()
			}
		}
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.countMessagesInChannel") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_message WHERE channel_id = $1")
					.bind("$1", channelId)
					.execute().mapToCount()
			}
		}
	}


	override fun getMessages(): Flux<MessageData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMessageRepository.getMessages") {
				it.createStatement("SELECT data FROM d4j_discord_message")
					.execute().deserializeManyFromData(MessageData::class.java, serde)
			}
		}
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMessageRepository.getMessagesInChannel") {
				it.createStatement("SELECT data FROM d4j_discord_message WHERE channel_id = $1")
					.bind("$1", channelId)
					.execute().deserializeManyFromData(MessageData::class.java, serde)
			}
		}
	}

	override fun getMessageById(messageId: Long): Mono<MessageData> {
		return Mono.defer {
			withConnection(factory, "PostgresMessageRepository.getMessageById") {
				it.createStatement("SELECT data FROM d4j_discord_message WHERE message_id = $1")
					.bind("$1", messageId)
					.execute().deserializeOneFromData(MessageData::class.java, serde)
			}
		}
	}

	override fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMessageRepository.getMessagesByIds") {
				it.createStatement("SELECT data FROM d4j_discord_message WHERE message_id = ANY($1)")
					.bind("$1", messageIds.toTypedArray())
					.execute().deserializeManyFromData(MessageData::class.java, serde)
			}
		}
	}
}
