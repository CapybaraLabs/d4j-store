package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.PostgresSerde
import dev.capybaralabs.d4j.store.postgres.deserializeManyFromData
import dev.capybaralabs.d4j.store.postgres.deserializeOneFromData
import dev.capybaralabs.d4j.store.postgres.executeConsuming
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
internal class UserRepository(private val factory: ConnectionFactory, private val serde: PostgresSerde) {


	init {
		withConnectionMany(factory) {
			it.createStatement(
				"""
				CREATE TABLE IF NOT EXISTS d4j_discord_user (
				    user_id BIGINT NOT NULL,
					data JSONB NOT NULL,
					CONSTRAINT d4j_discord_user_pkey PRIMARY KEY (user_id)
				)
				""".trimIndent()
			).executeConsuming()
		}.blockLast()
	}

	fun save(user: UserData): Mono<Void> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement(
					"""
					INSERT INTO d4j_discord_user VALUES ($1, $2 ::jsonb)
						ON CONFLICT (user_id) DO UPDATE SET data = $2::jsonb
					""".trimIndent()
				)
					.bind("$1", user.id().asLong())
					.bind("$2", serde.serializeToString(user))
					.executeConsumingSingle().then()
			}
		}
	}

	fun deleteById(userId: Long): Mono<Int> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("DELETE FROM d4j_discord_user WHERE user_id = $1")
					.bind("$1", userId)
					.executeConsumingSingle()
			}
		}
	}


	fun countUsers(): Mono<Long> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT count(*) AS count FROM d4j_discord_user")
					.execute().mapToCount()
			}
		}
	}

	fun getUsers(): Flux<UserData> {
		return Flux.defer {
			withConnectionMany(factory) {
				it.createStatement("SELECT data FROM d4j_discord_user")
					.execute().deserializeManyFromData(UserData::class.java, serde)
			}
		}
	}

	fun getUserById(userId: Long): Mono<UserData> {
		return Mono.defer {
			withConnection(factory) {
				it.createStatement("SELECT data FROM d4j_discord_user WHERE user_id = $1")
					.bind("$1", userId)
					.execute().deserializeOneFromData(UserData::class.java, serde)
			}
		}
	}
}
