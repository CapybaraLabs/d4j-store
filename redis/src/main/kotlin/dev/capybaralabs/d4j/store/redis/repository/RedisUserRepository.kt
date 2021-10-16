package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisUserRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), UserRepository {

	private val userKey = key("user")
	private val userOps = RedisHashOps(userKey, factory, Long::class.java, UserData::class.java)

	override fun save(user: UserData): Mono<Void> {
		return saveAll(listOf(user))
	}

	override fun saveAll(users: List<UserData>): Mono<Void> {
		if (users.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			userOps.putAll(users.associateBy { it.id().asLong() }).then()
		}
	}

	override fun deleteById(userId: Long): Mono<Long> {
		return deleteByIds(listOf(userId))
	}

	fun deleteByIds(userIds: Collection<Long>): Mono<Long> {
		return Mono.defer {
			userOps.remove(*userIds.toTypedArray())
		}
	}

	override fun countUsers(): Mono<Long> {
		return Mono.defer {
			userOps.size()
		}
	}

	override fun getUsers(): Flux<UserData> {
		return Flux.defer {
			userOps.values()
		}
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return Mono.defer {
			userOps.get(userId)
		}
	}
}
