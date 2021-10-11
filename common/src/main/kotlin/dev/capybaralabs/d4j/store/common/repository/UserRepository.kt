package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository {
	fun save(user: UserData): Mono<Void>
	fun saveAll(users: List<UserData>): Mono<Void>

	fun deleteById(userId: Long): Mono<Long>

	fun countUsers(): Mono<Long>

	fun getUsers(): Flux<UserData>
	fun getUserById(userId: Long): Mono<UserData>
}
