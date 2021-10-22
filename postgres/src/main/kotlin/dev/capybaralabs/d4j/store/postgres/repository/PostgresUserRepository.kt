package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.common.toLong
import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsumingAll
import dev.capybaralabs.d4j.store.postgres.executeConsumingSingle
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import dev.capybaralabs.d4j.store.postgres.withConnectionMany
import discord4j.discordjson.json.UserData
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Concerned with operations on the user table
 */
internal class PostgresUserRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) :
	UserRepository {


	init {
		withConnectionMany(factory, "PostgresUserRepository.init") {
			it.createStatement(
				"""
				CREATE UNLOGGED TABLE IF NOT EXISTS d4j_discord_user (
				    user_id BIGINT NOT NULL,
					data BYTEA NOT NULL,
					CONSTRAINT d4j_discord_user_pkey PRIMARY KEY (user_id)
				)
				""".trimIndent()
			).executeConsumingAll()
		}.blockLast()
	}

	override fun save(user: UserData): Mono<Void> {
		return saveAll(listOf(user)).then()
	}

	override fun saveAll(users: List<UserData>): Mono<Void> {
		if (users.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			withConnection(factory, "PostgresUserRepository.saveAll") {
				val statement = it.createStatement(
					"""
					INSERT INTO d4j_discord_user VALUES ($1, $2)
						ON CONFLICT (user_id) DO UPDATE SET data = $2
					""".trimIndent()
				)
				for (user in users) {
					statement
						.bind("$1", user.id().asLong())
						.bind("$2", serde.serialize(user))
						.add()
				}
				statement.executeConsumingAll().then()
			}
		}
	}

	override fun deleteById(userId: Long): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresUserRepository.deleteById") {
				it.createStatement("DELETE FROM d4j_discord_user WHERE user_id = $1")
					.bind("$1", userId)
					.executeConsumingSingle().toLong()
			}
		}
	}


	override fun countUsers(): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresUserRepository.countUsers") {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_user")
					.execute().mapToCount()
			}
		}
	}

	override fun getUsers(): Flux<UserData> {
		return Flux.defer {
			withConnectionMany(factory, "PostgresUserRepository.getUsers") {
				it.createStatement("SELECT data FROM d4j_discord_user")
					.execute().deserializeManyFromData(UserData::class.java, serde)
			}
		}
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return Mono.defer {
			withConnection(factory, "PostgresUserRepository.getUserById") {
				it.createStatement("SELECT data FROM d4j_discord_user WHERE user_id = $1")
					.bind("$1", userId)
					.execute().deserializeOneFromData(UserData::class.java, serde)
			}
		}
	}
}
