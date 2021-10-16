package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.tck.StoreLayoutResolver
import dev.capybaralabs.d4j.store.tck.StoreTck
import discord4j.common.store.api.layout.StoreLayout
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import reactor.tools.agent.ReactorDebugAgent


private const val redisPort = 6379
private val redisContainer = KGenericContainer("redis:6-alpine")
	.withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("Container.Redis")))
	.withExposedPorts(redisPort)

@ExtendWith(RedisContainerExtension::class)
class RedisStoreTck : StoreTck, StoreLayoutResolver() {

	private val storeLayout: RedisStoreLayout by lazy { RedisStoreLayout(connectionFactory()) }

	private fun connectionFactory(): ReactiveRedisConnectionFactory {
		ReactorDebugAgent.init()
		ReactorDebugAgent.processExistingClasses()

		redisContainer.start()
		val lettuce = LettuceConnectionFactory(redisContainer.host, redisContainer.getMappedPort(redisPort))
		lettuce.afterPropertiesSet()
		return lettuce
	}

	override fun storeLayout(): StoreLayout {
		return storeLayout
	}
}


//https://github.com/testcontainers/testcontainers-java/issues/318
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
class RedisContainerExtension : AfterAllCallback {

	override fun afterAll(context: ExtensionContext?) {
		redisContainer.stop()
	}
}
