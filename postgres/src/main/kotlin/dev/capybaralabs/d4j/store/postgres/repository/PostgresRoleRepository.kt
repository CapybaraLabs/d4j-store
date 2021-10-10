package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.RoleData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the role table
 */
internal class PostgresRoleRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	RoleRepository {

	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_role (
				    role_id BIGINT NOT NULL,
					guild_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					shard_index INT NOT NULL,
					CONSTRAINT d4j_discord_role_pkey PRIMARY KEY (role_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(guildId: Long, role: RoleData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(role), shardId).then()
	}

	override fun saveAll(guildId: Long, roles: List<RoleData>, shardId: Int): Mono<Void> {
		if (roles.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory) {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_role VALUES ($1, $2, $3::jsonb, $4)
						ON CONFLICT (role_id) DO UPDATE SET data = $3::jsonb, shard_index = $4
					""".trimIndent()
				)

				for (role in roles) {
					statement
						.bind("$1", role.id().asLong())
						.bind("$2", guildId)
						.bind("$3", serde.serializeToString(role))
						.bind("$4", shardId)
						.add()
				}
				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteById(roleId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_role WHERE role_id = $1")
					.bind("$1", roleId)
					.executeConsumingSingle()
			}
		}
	}

	override fun deleteByIds(roleIds: List<Long>): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it
					.createStatement("DELETE FROM d4j_discord_role WHERE role_id = ANY($1)")
					.bind("$1", roleIds.toTypedArray())
					.executeConsumingSingle()
			}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_role WHERE shard_index = $1")
					.bind("$1", shardId)
					.executeConsumingSingle()
			}
		}
	}

	override fun countRoles(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_role")
					.execute().mapToCount()
			}
		}
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_role WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().mapToCount()
			}
		}
	}

	override fun getRoles(): Flux<RoleData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_role")
					.execute().deserializeManyFromData(RoleData::class.java, serde)
			}
		}
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_role WHERE guild_id = $1")
					.bind("$1", guildId)
					.execute().deserializeManyFromData(RoleData::class.java, serde)
			}
		}
	}

	override fun getRoleById(roleId: Long): Mono<RoleData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_role WHERE role_id = $1")
					.bind("$1", roleId)
					.execute().deserializeOneFromData(RoleData::class.java, serde)
			}
		}
	}
}
