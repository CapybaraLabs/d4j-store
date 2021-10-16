package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.redis.RedisFactory
import org.springframework.data.redis.core.ScanOptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisHashOps<HK, HV>(private val key: String, factory: RedisFactory, hkClass: Class<HK>, hvClass: Class<HV>) {

	private val ops = factory.createRedisHashOperations(String::class.java, hkClass, hvClass)

	fun put(hashKey: HK, value: HV): Mono<Boolean> {
		return ops.put(key, hashKey, value)
	}

	fun putAll(data: Map<HK, HV>): Mono<Boolean> {
		if (data.isEmpty()) {
			return Mono.empty()
		}
		return ops.putAll(key, data)
	}

	fun remove(vararg hashKeys: HK): Mono<Long> {
		if (hashKeys.isEmpty()) {
			return Mono.empty()
		}
		return ops.remove(key, *hashKeys)
	}

	fun size(): Mono<Long> {
		return ops.size(key)
	}

	fun get(hashKey: HK): Mono<HV> {
		return ops.get(key, hashKey as Any)
	}

	fun values(): Flux<HV> {
		return ops.values(key)
	}

	fun multiGet(hashKeys: Collection<HK>): Mono<List<HV>> {
		if (hashKeys.isEmpty()) {
			return Mono.empty()
		}
		return ops.multiGet(key, hashKeys)
	}

	fun scan(options: ScanOptions): Flux<Map.Entry<HK, HV>> {
		return ops.scan(key, options)
	}
}
