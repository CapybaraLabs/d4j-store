package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface MemberRepository {
	fun save(guildId: Long, member: MemberData, shardIndex: Int): Mono<Void>

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	fun saveAll(guildId: Long, members: List<MemberData>, shardIndex: Int): Mono<Void>

	fun deleteById(guildId: Long, userId: Long): Mono<Int>
	fun deleteByGuildId(guildId: Long): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>

	fun countMembers(): Mono<Long>
	fun countMembersInGuild(guildId: Long): Mono<Long>

	fun getMembers(): Flux<MemberData>
	fun getExactMembersInGuild(guildId: Long): Flux<MemberData>
	fun getMemberById(guildId: Long, userId: Long): Mono<MemberData>
	fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>>
}
