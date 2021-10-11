package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisUserRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), UserRepository {

	private val userKey = key("user")
	private val userOps = factory.createRedisHashOperations<String, Long, UserData>()

	override fun save(user: UserData): Mono<Void> {
		return saveAll(listOf(user))
	}

	override fun saveAll(users: List<UserData>): Mono<Void> {
		return Mono.defer {
			userOps.putAll(userKey, users.associateBy { it.id().asLong() }).then()
		}
	}

	override fun deleteById(userId: Long): Mono<Long> {
		return Mono.defer {
			userOps.remove(userKey, userId)
		}
	}

	override fun countUsers(): Mono<Long> {
		return Mono.defer {
			userOps.size(userKey)
		}
	}

	override fun getUsers(): Flux<UserData> {
		return Flux.defer {
			userOps.values(userKey)
		}
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return Mono.defer {
			userOps.get(userKey, userId)
		}
	}
}
