package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
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
internal class PostgresMemberRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	MemberRepository {

	init {
		withConnectionMany(factory, "PostgresMemberRepository.init") {
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
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void> {
		return saveAll(mapOf(Pair(guildId, listOf(member))), shardId)
	}

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	override fun saveAll(membersByGuild: Map<Long, List<MemberData>>, shardId: Int): Mono<Void> {
		val filtered = membersByGuild.filter { it.value.isNotEmpty() }
		if (filtered.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.saveAll") {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_member VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (guild_id, user_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)

				for (guildMembers in filtered.entries) {
					val guildId = guildMembers.key
					for (member in guildMembers.value) {
						statement
							.bind("$1", member.user().id().asLong())
							.bind("$2", guildId)
							.bind("$3", serde.serializeToString(member))
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
			withConnection(factory, "PostgresMemberRepository.deleteById") {
				it.createStatement("DELETE FROM d4j_discord_member WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.deleteByGuildId") {
				it.createStatement("DELETE FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.deleteByShardId") {
				it.createStatement("DELETE FROM d4j_discord_member WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle().toLong()
			}
		}
	}

	override fun countMembers(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.countMembers") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_member")
					.execute().mapToCount()
			}
		}
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.countMembersInGuild") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getMembers(): Flux<MemberData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMemberRepository.getMembers") {
				it.createStatement("SELECT data FROM d4j_discord_member")
					.execute().deserializeManyFromData(MemberData::class.java, serde)
			}
		}
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMemberRepository.getExactMembersInGuild") {
				it.createStatement("SELECT data FROM d4j_discord_member WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(MemberData::class.java, serde)
			}
		}
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.defer {
			withConnection(factory, "PostgresMemberRepository.getMemberById") {
				it.createStatement("SELECT data FROM d4j_discord_member WHERE guild_id = $1 AND user_id = $2")
					.bind("$1", guildId)
					.bind("$2", userId)
					.execute().deserializeOneFromData(MemberData::class.java, serde)
			}
		}
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresMemberRepository.getMembersByUserId") { connection ->
				connection.createStatement("SELECT guild_id, data FROM d4j_discord_member WHERE user_id = $1")
					.bind("$1", userId)
					.execute().toFlux()
					.flatMap {
						it.map { row, _ ->
							Pair(
								row.get("guild_id", java.lang.Long::class.java)!!.toLong(),
								row.get("data", ByteArray::class.java)!!
							)
						}
					}
					.map { Pair(it.first, serde.deserialize(it.second, MemberData::class.java)) }
			}
		}
	}
}
