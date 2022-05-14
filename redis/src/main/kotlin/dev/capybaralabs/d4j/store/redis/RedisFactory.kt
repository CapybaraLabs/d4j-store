package dev.capybaralabs.d4j.store.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import org.springframework.data.redis.core.ReactiveZSetOperations
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

class RedisFactory(private val connectionFactory: ReactiveRedisConnectionFactory, private val objectMapper: ObjectMapper) {

	inline fun <reified K, reified V> createRedisOperations(): ReactiveRedisOperations<K, V> {
		return createRedisOperations(K::class.java, V::class.java)
	}

	fun <K, V> createRedisOperations(kClass: Class<K>, vClass: Class<V>): ReactiveRedisOperations<K, V> {
		val builder = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
		val context = builder
			.key(serializer(kClass))
			.value(serializer(vClass))
			.build()
		return ReactiveRedisTemplate(connectionFactory, context)
	}

	inline fun <reified K, reified HK, reified HV> createRedisHashOperations(): ReactiveHashOperations<K, HK, HV> {
		return createRedisHashOperations(K::class.java, HK::class.java, HV::class.java)
	}

	fun <K, HK, HV> createRedisHashOperations(kClass: Class<K>, hkClass: Class<HK>, hvClass: Class<HV>): ReactiveHashOperations<K, HK, HV> {
		val context = RedisSerializationContext.newSerializationContext<K, HV>(StringRedisSerializer())
			.key(serializer(kClass))
			.hashKey(serializer(hkClass))
			.hashValue(serializer(hvClass))
			.build()
		return ReactiveRedisTemplate(connectionFactory, context).opsForHash()
	}

	inline fun <reified K, reified V> createRedisZSetOperations(): ReactiveZSetOperations<K, V> {
		return createRedisZSetOperations<K, V>(K::class.java, V::class.java)
	}

	fun <K, V> createRedisZSetOperations(kClass: Class<K>, vClass: Class<V>): ReactiveZSetOperations<K, V> {
		val context = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
			.key(serializer(kClass))
			.value(serializer(vClass))
			.build()

		return ReactiveRedisTemplate(connectionFactory, context).opsForZSet()
	}

	inline fun <reified K, reified V> createRedisSetOperations(): ReactiveSetOperations<K, V> {
		return createRedisSetOperations(K::class.java, V::class.java)
	}

	fun <K, V> createRedisSetOperations(kClass: Class<K>, vClass: Class<V>): ReactiveSetOperations<K, V> {
		val context = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
			.key(serializer(kClass))
			.value(serializer(vClass))
			.build()

		return ReactiveRedisTemplate(connectionFactory, context).opsForSet()
	}

	internal fun <T> serializer(clazz: Class<T>): RedisSerializer<T> {
		return if (clazz == String::class.java) {
			@Suppress("UNCHECKED_CAST")
			StringRedisSerializer() as RedisSerializer<T>
		} else {
			val serializer = Jackson2JsonRedisSerializer(clazz)
			serializer.setObjectMapper(objectMapper)
			serializer
		}
	}

	internal fun <T> genericSerializer(): RedisSerializer<Any> {
		return GenericJackson2JsonRedisSerializer(objectMapper)
	}
}
