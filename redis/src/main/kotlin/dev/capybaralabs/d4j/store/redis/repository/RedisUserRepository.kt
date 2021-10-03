package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.UserData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class RedisUserRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), UserRepository {

	private val hash = hash("user")
	private val hashOps = factory.createRedisHashOperations<String, Long, UserData>()

	override fun save(user: UserData): Mono<Void> {
		return saveAll(listOf(user)).then()
	}

	override fun saveAll(users: List<UserData>): Flux<Int> {
		return Flux.defer {
			hashOps.putAll(hash, users.associateBy { it.id().asLong() })
				.map { if (it) 1 else 0 } // TODO rethink the signature of the method, it doesnt really make sense here
				.toFlux()
		}
	}

	override fun deleteById(userId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, userId)
				.map { toIntExact(it) }
		}
	}

	override fun countUsers(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun getUsers(): Flux<UserData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return Mono.defer {
			hashOps.get(hash, userId)
		}
	}
}
