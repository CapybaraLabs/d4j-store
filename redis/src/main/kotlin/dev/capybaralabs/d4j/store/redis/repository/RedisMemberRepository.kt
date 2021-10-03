package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MemberData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class RedisMemberRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MemberRepository {

	private val hash = hash("member")
	private val hashOps = factory.createRedisHashOperations<String, String, MemberData>()

	private fun memberKey(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(guildId: Long, member: MemberData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(member), shardIndex).then()
	}

	override fun saveAll(guildId: Long, members: List<MemberData>, shardIndex: Int): Flux<Int> {
		return Flux.defer {
			hashOps.putAll(hash, members.associateBy { memberKey(guildId, it.user().id().asLong()) })
				.map { if (it) 1 else 0 } // TODO rethink the signature of the method, it doesnt really make sense here
				.toFlux()
		}

	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, memberKey(guildId, userId))
				.map { StrictMath.toIntExact(it) }
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		// uuuhhhhhh...ask guild about the ids?
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		// ask shard about the ids
		TODO("Not yet implemented")
	}

	override fun countMembers(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		// ask guilds about ids
		TODO("Not yet implemented")
	}

	override fun getMembers(): Flux<MemberData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		// ask guild about ids
		TODO("Not yet implemented")
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.defer {
			hashOps.get(hash, memberKey(guildId, userId))
		}
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		TODO("Not yet implemented")
	}
}
