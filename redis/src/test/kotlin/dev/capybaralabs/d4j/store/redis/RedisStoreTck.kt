package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import dev.capybaralabs.d4j.store.tck.StoreLayoutProvider
import dev.capybaralabs.d4j.store.tck.StoreLayoutResolver
import dev.capybaralabs.d4j.store.tck.StoreTck
import discord4j.common.store.api.layout.StoreLayout
import java.util.EnumSet
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
class RedisStoreTck : StoreTck, StoreLayoutResolver(), StoreLayoutProvider {

	private val connectionFactory: ReactiveRedisConnectionFactory by lazy {
		ReactorDebugAgent.init()
		ReactorDebugAgent.processExistingClasses()

		redisContainer.start()
		val lettuce = LettuceConnectionFactory(redisContainer.host, redisContainer.getMappedPort(redisPort))
		lettuce.afterPropertiesSet()
		return@lazy lettuce
	}
	private val storeLayout: RedisStoreLayout by lazy { RedisStoreLayout(connectionFactory) }

	override fun storeLayoutProvider(): StoreLayoutProvider {
		return this
	}

	override fun defaultLayout(): StoreLayout {
		return storeLayout
	}

	override fun withFlags(storeFlags: EnumSet<StoreFlag>): StoreLayout {
		return RedisStoreLayout(connectionFactory, storeFlags)
	}
}


//https://github.com/testcontainers/testcontainers-java/issues/318
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
class RedisContainerExtension : AfterAllCallback {

	override fun afterAll(context: ExtensionContext?) {
		redisContainer.stop()
	}
}
