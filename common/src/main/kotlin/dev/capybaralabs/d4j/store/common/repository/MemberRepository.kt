package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface MemberRepository {
	fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void>

	// TODO we are potentially duplicating .user() data here, is there a way to avoid it?
	fun saveAll(membersByGuild: Map<Long, List<MemberData>>, shardId: Int): Mono<Void>

	fun deleteById(guildId: Long, userId: Long): Mono<Long>
	fun deleteByGuildId(guildId: Long): Mono<Long>
	fun deleteByShardId(shardId: Int): Mono<Long>

	fun countMembers(): Mono<Long>
	fun countMembersInGuild(guildId: Long): Mono<Long>

	fun getMembers(): Flux<MemberData>
	fun getExactMembersInGuild(guildId: Long): Flux<MemberData>
	fun getMemberById(guildId: Long, userId: Long): Mono<MemberData>
	fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>>  // by guildId
}
