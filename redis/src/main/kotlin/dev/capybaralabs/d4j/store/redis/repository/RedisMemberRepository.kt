package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMemberRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MemberRepository {

	private val memberKey = key("member")
	private val memberOps = factory.createRedisHashOperations<String, String, MemberData>()

	private fun memberId(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(member), shardId)
	}

	override fun saveAll(guildId: Long, members: List<MemberData>, shardId: Int): Mono<Void> {
		return Mono.defer {
			memberOps.putAll(memberKey, members.associateBy { memberId(guildId, it.user().id().asLong()) }).then()
		}

	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			memberOps.remove(memberKey, memberId(guildId, userId))
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		// uuuhhhhhh...ask guild about the ids?
		TODO("Not yet implemented")
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// ask shard about the ids
		TODO("Not yet implemented")
	}

	override fun countMembers(): Mono<Long> {
		return Mono.defer {
			memberOps.size(memberKey)
		}
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		// ask guilds about ids
		TODO("Not yet implemented")
	}

	override fun getMembers(): Flux<MemberData> {
		return Flux.defer {
			memberOps.values(memberKey)
		}
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		// ask guild about ids
		TODO("Not yet implemented")
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.defer {
			memberOps.get(memberKey, memberId(guildId, userId))
		}
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		TODO("Not yet implemented")
	}
}
