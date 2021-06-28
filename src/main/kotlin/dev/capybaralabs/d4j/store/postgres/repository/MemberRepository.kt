package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsuming
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.MemberData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

/**
 * Concerned with operations on the member table
 */
internal class MemberRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_member (
					user_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_member_pkey PRIMARY KEY (guild_id, user_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	fun save(guildId: Long, member: MemberData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(member), shardIndex).then()
	}

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	fun saveAll(guildId: Long, members: List<MemberData>, shardIndex: Int): Flux<Int> {
		if (members.isEmpty()) {
			return Flux.empty()
		}

		return Flux.defer {
			withConnectionMany(factory) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_member VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (guild_id, user_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)

				for (member in members) {
					statement
						.bind("$1", member.user().id().asLong())
						.bind("$2", guildId)
						.bind("$3", serde.serializeToString(member))
						.bind("$4", shardIndex)
						.add()
				}

				statement.executeConsuming()
			}
		}
	}


	fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_member WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.executeConsumingSingle()
			}
		}
	}

	fun deleteByGuildId(guildId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle()
			}
		}
	}

	fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_member WHERE shard_index = $1")
					.bind("$1", shardIndex)
					.executeConsumingSingle()
			}
		}
	}

	fun countMembers(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_member")
					.execute().mapToCount()
			}
		}
	}

	fun countMembersInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	fun getMembers(): Flux<MemberData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_member")
					.execute().deserializeManyFromData(MemberData::class.java, serde)
			}
		}
	}

	fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(MemberData::class.java, serde)
			}
		}
	}

	fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_member WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(MemberData::class.java, serde)
			}
		}
	}

	fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		return Flux.defer {
			withConnectionMany(factory) { connection ->
				connection.createStatement("SELECT guild_id, data FROM d4j_discord_member WHERE user_id = $1")
					.bind("$1", userId)
					.execute().toFlux()
					.flatMap { it.map { row, _ -> Pair(row.get("guild_id", java.lang.Long::class.java).toLong(), row.get("data", ByteArray::class.java)) } }
					.map { Pair(it.first, serde.deserialize(it.second, MemberData::class.java)) }
			}
		}
	}
}
