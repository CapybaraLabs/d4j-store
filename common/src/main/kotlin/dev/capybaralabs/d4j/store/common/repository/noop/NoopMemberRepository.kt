package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopMemberRepository : MemberRepository {
	override fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(membersByGuild: Map<Long, List<MemberData>>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countMembers(): Mono<Long> {
		return Mono.empty()
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun getMembers(): Flux<MemberData> {
		return Flux.empty()
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return Flux.empty()
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.empty()
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		return Flux.empty()
	}
}
