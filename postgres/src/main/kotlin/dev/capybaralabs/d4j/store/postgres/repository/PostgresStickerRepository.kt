package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.StickerRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.StickerData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the sticker table
 */
internal class PostgresStickerRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	StickerRepository {

	init {
		withConnectionMany(factory, "PostgresStickerRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_sticker (
				    sticker_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_sticker_pkey PRIMARY KEY (sticker_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, sticker: StickerData, shardId: Int): Mono<Void> {
		val data = StickerData.builder()
			.from(sticker)
			.guildId(guildId)
			.build()

		return saveAll(listOf(data), shardId)
	}

	override fun saveAll(stickers: List<StickerData>, shardId: Int): Mono<Void> {
		val filtered = stickers.filter { it.guildId().isPresent() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.saveAll", filtered.size) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_sticker VALUES ($1, $2, $3, $4)
						ON CONFLICT (sticker_id) DO UPDATE SET data = $3, shard_index = $4
					""".trimIndent()
				)

				for (sticker in filtered) {
					statement
						.bind("$1", sticker.id().asLong())
						.bind("$2", sticker.guildId().get().asLong())
						.bind("$3", serde.serialize(sticker))
						.bind("$4", shardId)
						.add()
				}
				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteByIds(stickerIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.deleteByIds") {
				it.createStatement("DELETE FROM d4j_discord_sticker WHERE sticker_id = ANY($1) AND guild_id = $2")
					.bind("$1", stickerIds.toTypedArray())
					.bind("$2", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepositoryl.deleteByGuildId") {
				it.createStatement("DELETE FROM d4j_discord_sticker WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}


	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_sticker WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countStickers(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.countStickers") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_sticker")
					.execute().mapToCount()
			}
		}
	}

	override fun countStickersInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.countStickersInGuild") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_sticker WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getStickers(): Flux<StickerData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresStickerRepository.getStickers") {
				it.createStatement("SELECT data FROM d4j_discord_sticker")
					.execute().deserializeManyFromData(StickerData::class.java, serde)
			}
		}
	}

	override fun getStickersInGuild(guildId: Long): Flux<StickerData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresStickerRepository.getStickersInGuild") {
				it.createStatement("SELECT data FROM d4j_discord_sticker WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(StickerData::class.java, serde)
			}
		}
	}

	override fun getStickerById(guildId: Long, stickerId: Long): Mono<StickerData> {
		return Mono.defer {
			withConnection(factory, "PostgresStickerRepository.getStickerById") {
				it.createStatement("SELECT data FROM d4j_discord_sticker WHERE guild_id = $1 AND sticker_id = $2")
					.bind("$1", guildId)
					.bind("$2", stickerId)
					.execute().deserializeOneFromData(StickerData::class.java, serde)
			}
		}
	}
}
