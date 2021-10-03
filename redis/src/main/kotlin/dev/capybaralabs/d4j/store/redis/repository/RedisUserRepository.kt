package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisUserRepository : UserRepository {
	override fun save(user: UserData): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(users: List<UserData>): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteById(userId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countUsers(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getUsers(): Flux<UserData> {
		TODO("Not yet implemented")
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		TODO("Not yet implemented")
	}
}
