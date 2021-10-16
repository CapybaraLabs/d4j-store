package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.redis.RedisFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun oneWayIndex(key: String, factory: RedisFactory): OneWayIndex<Long> {
	return oneWayIndex(key, factory, Long::class.java)
}

fun <V> oneWayIndex(key: String, factory: RedisFactory, valueClass: Class<V>): OneWayIndex<V> {
	return OneWayIndex(key, factory, valueClass)
}

/**
 * Secondary Redis index implementation.
 *
 * One-way index: Simple set, guilds ids are 64bit and CANNOT be used as a group/score for a proper two-way index.
 *
 * Use [TwoWayIndex] to build indices that are bi-directional.
 */
class OneWayIndex<V>(private val keyPrefix: String, factory: RedisFactory, valueClass: Class<V>) {

	private val ops = factory.createRedisOperations(String::class.java, valueClass)
	private val setOps = factory.createRedisSetOperations(String::class.java, valueClass)

	private fun key(groupId: Long): String {
		return "$keyPrefix:$groupId"
	}

	fun addElements(groupId: Long, vararg elements: V): Mono<Long> {
		if (elements.isEmpty()) {
			return Mono.empty()
		}
		return setOps.add(key(groupId), *elements)
	}

	fun removeElements(groupId: Long, vararg elements: V): Mono<Long> {
		if (elements.isEmpty()) {
			return Mono.empty()
		}
		return setOps.remove(key(groupId), *elements)
	}

	fun countElementsInGroup(groupId: Long): Mono<Long> {
		return setOps.size(key(groupId))
	}

	fun getElementsInGroup(groupId: Long): Flux<V> {
		return setOps.members(key(groupId))
	}

	fun getElementsInGroups(groupIds: Collection<Long>): Flux<V> {
		if (groupIds.isEmpty()) {
			return Flux.empty()
		}
		return setOps.union(groupIds.map { key(it) })
	}

	fun deleteGroup(groupId: Long): Mono<Boolean> {
		return setOps.delete(key(groupId))
	}

	fun deleteGroups(groupIds: Collection<Long>): Mono<Long> {
		if (groupIds.isEmpty()) {
			return Mono.empty()
		}
		return ops.delete(*groupIds.map { key(it) }.toTypedArray())
	}
}
