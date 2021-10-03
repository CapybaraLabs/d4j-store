package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.common.CommonDataAccessor
import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.Serde
import dev.capybaralabs.d4j.store.redis.repository.RedisChannelRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisEmojiRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisGuildRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisMemberRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisMessageRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisPresenceRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisRoleRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisUserRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisVoiceStateRepository
import discord4j.common.store.api.layout.DataAccessor
import discord4j.common.store.api.layout.GatewayDataUpdater
import discord4j.common.store.api.layout.StoreLayout
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory

class RedisStoreLayout(connectionFactory: ReactiveRedisConnectionFactory) : StoreLayout {

	private val factory = RedisFactory(connectionFactory, Serde.objectMapper())

	private val repositories = RedisRepositories(
		RedisChannelRepository(factory),
		RedisEmojiRepository(),
		RedisGuildRepository(),
		RedisMemberRepository(),
		RedisMessageRepository(),
		RedisPresenceRepository(),
		RedisRoleRepository(),
		RedisUserRepository(),
		RedisVoiceStateRepository(),
	)

	private val redisDataAccessor: CommonDataAccessor = CommonDataAccessor(repositories)
	private val redisGatewayDataUpdater: CommonGatewayDataUpdater = CommonGatewayDataUpdater(repositories)

	override fun getDataAccessor(): DataAccessor {
		return redisDataAccessor
	}

	override fun getGatewayDataUpdater(): GatewayDataUpdater {
		return redisGatewayDataUpdater
	}
}
