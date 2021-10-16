package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MemberData
import org.springframework.data.redis.core.ScanOptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisMemberRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MemberRepository {

	private val memberKey = key("member")
	private val memberOps = RedisHashOps(memberKey, factory, String::class.java, MemberData::class.java)

	private val shardIndex = twoWayIndex("$memberKey:shard-index", factory, String::class.java)
	private val guildIndex = oneWayIndex("$memberKey:guild-index", factory)
	private val gShardIndex = twoWayIndex("$memberKey:guild-shard-index", factory)

	private fun memberId(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	private fun userPattern(userId: Long): String {
		return "*:$userId"
	}

	private fun guildPattern(guildId: Long): String {
		return "$guildId:$*"
	}

	private fun extractGuildId(memberId: String): Long {
		return memberId.split(":")[0].toLong()
	}

	private fun extractUserId(memberId: String): Long {
		return memberId.split(":")[1].toLong()
	}

	override fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(member), shardId)
	}

	override fun saveAll(guildId: Long, members: List<MemberData>, shardId: Int): Mono<Void> {
		if (members.isEmpty()) {
			return Mono.empty()
		}
		return Mono.defer {
			val memberMap = members.associateBy { memberId(guildId, it.user().id().asLong()) }
			val userIds = members.map { it.user().id().asLong() }

			val addToShardIndex = shardIndex.addElements(shardId, memberMap.keys)
			val addToGuildIndex = guildIndex.addElements(guildId, *userIds.toTypedArray())
			val addToGuildShardIndex = gShardIndex.addElements(shardId, listOf(guildId))

			val save = memberOps.putAll(memberMap)

			Mono.`when`(addToShardIndex, addToGuildIndex, addToGuildShardIndex, save)
		}

	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		val memberId = memberId(guildId, userId)
		return Mono.defer {
			val removeFromShardIndex = shardIndex.removeElements(memberId)
			val removeFromGuildIndex = guildIndex.removeElements(guildId, userId)

			val remove = memberOps.remove(memberId)

			Mono.`when`(removeFromShardIndex, removeFromGuildIndex)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { userIds ->
					val memberIds = userIds.map { memberId(guildId, it) }
					val removeFromShardIndex = shardIndex.removeElements(*memberIds.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = memberOps.remove(*memberIds.toTypedArray())

					Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			val removeFromGuildIndices = gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { gShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
				.flatMap { guildIds -> guildIndex.deleteGroups(guildIds) }

			val delete = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
				.flatMap { memberOps.remove(*it.toTypedArray()) }

			removeFromGuildIndices.then(delete)
		}
	}

	override fun countMembers(): Mono<Long> {
		return Mono.defer {
			memberOps.size()
		}
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun getMembers(): Flux<MemberData> {
		return Flux.defer {
			memberOps.values()
		}
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { memberOps.multiGet(it.map { userId -> memberId(guildId, userId) }) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return Mono.defer {
			memberOps.get(memberId(guildId, userId))
		}
	}

	override fun getMembersByUserId(userId: Long): Flux<Pair<Long, MemberData>> {
		return Flux.defer {
			val userScan = ScanOptions.scanOptions()
				.match(userPattern(userId))
				.count(1000) // sparse dataset, use a large batch size
				.build()
			memberOps.scan(userScan)
				.map { Pair(extractGuildId(it.key), it.value) }
		}
	}

	fun getUserIdsOnShard(shardId: Int): Flux<Long> {
		return shardIndex.getElementsByGroup(shardId)
			.map { extractUserId(it) }
	}

	fun getUserIdsOnOtherShards(shardId: Int): Flux<Long> {
		return shardIndex.getElementsNotInGroup(shardId)
			.map { extractUserId(it) }
	}
}
