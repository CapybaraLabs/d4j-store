package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.redis.RedisFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ZSetOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Secondary Redis index implementation.
 *
 * Two-way index: Uses a Zset, because shards ids are integers which CAN be used as a score.
 *
 * Use [OneWayIndex] to build snowflakes-to-snowflakes indices.
 */
class TwoWayIndex(private val key: String, factory: RedisFactory) {

	private val zsetOps = factory.createRedisZSetOperations<String, Long>()

	fun addElements(groupId: Int, elements: Collection<Long>): Mono<Long> {
		if (elements.isEmpty()) {
			return Mono.empty()
		}
		val tuples = elements.map { ZSetOperations.TypedTuple.of(it, groupId.toDouble()) }
		return zsetOps.addAll(key, tuples)
	}

	fun removeElements(elements: Collection<Long>): Mono<Long> {
		return zsetOps.remove(key, *elements.toTypedArray())
	}

	fun getElementsByGroup(groupId: Int): Flux<Long> {
		return zsetOps.rangeByScore(key, Range.just(groupId.toDouble()))
	}

	fun deleteByGroupId(groupId: Int): Mono<Long> {
		return zsetOps.removeRangeByScore(key, Range.just(groupId.toDouble()))
	}
}
