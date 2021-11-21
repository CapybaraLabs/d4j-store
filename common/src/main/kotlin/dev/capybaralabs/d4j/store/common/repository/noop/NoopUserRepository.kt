package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.UserRepository
import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopUserRepository : UserRepository {
	override fun save(user: UserData): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(users: List<UserData>): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteById(userId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun countUsers(): Mono<Long> {
		return Mono.just(0)
	}

	override fun getUsers(): Flux<UserData> {
		return Flux.empty()
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return Mono.empty()
	}
}
