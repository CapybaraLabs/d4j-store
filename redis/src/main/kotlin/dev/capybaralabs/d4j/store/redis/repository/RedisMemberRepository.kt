package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMemberRepository : MemberRepository {
	override fun save(guildId: Long, member: MemberData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(guildId: Long, members: List<MemberData>, shardIndex: Int): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countMembers(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getMembers(): Flux<MemberData> {
		TODO("Not yet implemented")
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		TODO("Not yet implemented")
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		TODO("Not yet implemented")
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		TODO("Not yet implemented")
	}
}
