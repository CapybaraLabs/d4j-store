package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.redis.RedisFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ZSetOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


fun twoWayIndex(key: String, factory: RedisFactory): TwoWayIndex<Long> {
	return twoWayIndex(key, factory, Long::class.java)
}

fun <V> twoWayIndex(key: String, factory: RedisFactory, valueClass: Class<V>): TwoWayIndex<V> {
	return TwoWayIndex(key, factory, valueClass)
}

/**
 * Secondary Redis index implementation.
 *
 * Two-way index: Uses a Zset, because shards ids are integers which CAN be used as a score.
 *
 * Use [OneWayIndex] to build snowflakes-to-snowflakes indices.
 */
class TwoWayIndex<V>(private val key: String, factory: RedisFactory, valueClass: Class<V>) {

	private val zsetOps = factory.createRedisZSetOperations(String::class.java, valueClass)


	fun addElements(groupId: Int, elements: Collection<V>): Mono<Long> {
		if (elements.isEmpty()) {
			return Mono.empty()
		}
		val tuples = elements.map { ZSetOperations.TypedTuple.of(it, groupId.toDouble()) }
		return zsetOps.addAll(key, tuples)
	}

	fun removeElements(vararg elements: V): Mono<Long> {
		if (elements.isEmpty()) {
			return Mono.empty()
		}
		return zsetOps.remove(key, *elements)
	}

	fun getElementsByGroup(groupId: Int): Flux<V> {
		return zsetOps.rangeByScore(key, Range.just(groupId.toDouble()))
	}

	fun deleteByGroupId(groupId: Int): Mono<Long> {
		return zsetOps.removeRangeByScore(key, Range.just(groupId.toDouble()))
	}
}
