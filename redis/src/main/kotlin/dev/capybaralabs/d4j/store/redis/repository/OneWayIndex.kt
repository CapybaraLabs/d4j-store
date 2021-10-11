package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.redis.RedisFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Secondary Redis index implementation.
 *
 * One-way index: Simple set, guilds ids are 64bit and CANNOT be used as a group/score for a proper two-way index.
 *
 * Use [TwoWayIndex] to build indices that are bi-directional.
 */
class OneWayIndex(private val keyPrefix: String, factory: RedisFactory) {

	private val ops = factory.createRedisOperations<String, Long>()
	private val setOps = factory.createRedisSetOperations<String, Long>()

	private fun key(groupId: Long): String {
		return "$keyPrefix:$groupId"
	}

	fun addElements(groupId: Long, elements: Collection<Long>): Mono<Long> {
		return setOps.add(key(groupId), *elements.toTypedArray())
	}

	fun removeElements(groupId: Long, elements: Collection<Long>): Mono<Long> {
		return setOps.remove(key(groupId), *elements.toTypedArray())
	}

	fun countElementsInGroup(groupId: Long): Mono<Long> {
		return setOps.size(key(groupId))
	}

	fun getElementsInGroup(groupId: Long): Flux<Long> {
		return setOps.members(key(groupId))
	}

	fun getElementsInGroups(groupIds: Collection<Long>): Flux<Long> {
		return setOps.union(groupIds.map { key(it) })
	}

	fun deleteGroup(groupId: Long): Mono<Boolean> {
		return setOps.delete(key(groupId))
	}

	fun deleteGroups(groupIds: Collection<Long>): Mono<Long> {
		return ops.delete(*groupIds.map { key(it) }.toTypedArray())
	}
}
