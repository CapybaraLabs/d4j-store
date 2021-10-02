package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.UserData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository {
	fun save(user: UserData): Mono<Void>
	fun saveAll(users: List<UserData>): Flux<Int>
	fun deleteById(userId: Long): Mono<Int>
	fun countUsers(): Mono<Long>
	fun getUsers(): Flux<UserData>
	fun getUserById(userId: Long): Mono<UserData>
}
