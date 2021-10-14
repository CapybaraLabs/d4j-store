package dev.capybaralabs.d4j.store.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import org.springframework.data.redis.core.ReactiveZSetOperations
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

class RedisFactory(val connectionFactory: ReactiveRedisConnectionFactory, val objectMapper: ObjectMapper) {

	inline fun <reified K, reified V> createRedisOperations(): ReactiveRedisOperations<K, V> {
		return createRedisOperations(K::class.java, V::class.java)
	}

	fun <K, V> createRedisOperations(kClass: Class<K>, vClass: Class<V>): ReactiveRedisOperations<K, V> {
		val keySerializer = Jackson2JsonRedisSerializer(kClass)
		val valueSerializer = Jackson2JsonRedisSerializer(vClass)
		valueSerializer.setObjectMapper(objectMapper)
		val builder = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
		val context = builder
			.key(keySerializer)
			.value(valueSerializer)
			.build()
		return ReactiveRedisTemplate(connectionFactory, context)
	}

	inline fun <reified K, reified HK, reified HV> createRedisHashOperations(): ReactiveHashOperations<K, HK, HV> {
		return createRedisHashOperations(K::class.java, HK::class.java, HV::class.java)
	}

	fun <K, HK, HV> createRedisHashOperations(kClass: Class<K>, hkClass: Class<HK>, hvClass: Class<HV>): ReactiveHashOperations<K, HK, HV> {
		val keySerializer = Jackson2JsonRedisSerializer(kClass)
		val hashKeySerializer = Jackson2JsonRedisSerializer(hkClass)
		val hashValueSerializer = Jackson2JsonRedisSerializer(hvClass)
		hashValueSerializer.setObjectMapper(objectMapper)

		val context = RedisSerializationContext.newSerializationContext<K, HV>(StringRedisSerializer())
			.key(keySerializer)
			.hashKey(hashKeySerializer)
			.hashValue(hashValueSerializer)
			.build()
		return ReactiveRedisTemplate(connectionFactory, context).opsForHash()
	}

	inline fun <reified K, reified V> createRedisZSetOperations(): ReactiveZSetOperations<K, V> {
		return createRedisZSetOperations<K, V>(K::class.java, V::class.java)
	}

	fun <K, V> createRedisZSetOperations(kClass: Class<K>, vClass: Class<V>): ReactiveZSetOperations<K, V> {
		val keySerializer = Jackson2JsonRedisSerializer(kClass)
		val valueSerializer = Jackson2JsonRedisSerializer(vClass)
		valueSerializer.setObjectMapper(objectMapper)

		val context = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
			.key(keySerializer)
			.value(valueSerializer)
			.build()

		return ReactiveRedisTemplate(connectionFactory, context).opsForZSet()
	}

	inline fun <reified K, reified V> createRedisSetOperations(): ReactiveSetOperations<K, V> {
		return createRedisSetOperations(K::class.java, V::class.java)
	}

	fun <K, V> createRedisSetOperations(kClass: Class<K>, vClass: Class<V>): ReactiveSetOperations<K, V> {
		val keySerializer = Jackson2JsonRedisSerializer(kClass)
		val valueSerializer = Jackson2JsonRedisSerializer(vClass)
		valueSerializer.setObjectMapper(objectMapper)

		val context = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
			.key(keySerializer)
			.value(valueSerializer)
			.build()

		return ReactiveRedisTemplate(connectionFactory, context).opsForSet()
	}

}
