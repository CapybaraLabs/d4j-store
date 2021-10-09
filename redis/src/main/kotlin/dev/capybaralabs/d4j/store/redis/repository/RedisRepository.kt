package dev.capybaralabs.d4j.store.redis.repository

abstract class RedisRepository(private val prefix: String) {

	internal fun key(suffix: String): String {
		return "$prefix:$suffix"
	}

}
