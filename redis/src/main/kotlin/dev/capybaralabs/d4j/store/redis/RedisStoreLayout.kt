package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.common.CommonDataAccessor
import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.Serde
import dev.capybaralabs.d4j.store.common.repository.flag.FlaggedRepositories
import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
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
import java.util.EnumSet
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory

class RedisStoreLayout(
	connectionFactory: ReactiveRedisConnectionFactory,
	storeFlags: EnumSet<StoreFlag> = StoreFlag.all,
	prefix: String = "d4j_capy",
) : StoreLayout {

	private val factory = RedisFactory(connectionFactory, Serde.objectMapper())

	private val repositories = FlaggedRepositories(
		storeFlags, RedisRepositories(
			RedisChannelRepository(prefix, factory),
			RedisEmojiRepository(prefix, factory),
			RedisGuildRepository(prefix, factory),
			RedisMemberRepository(prefix, factory),
			RedisMessageRepository(prefix, factory),
			RedisPresenceRepository(prefix, factory),
			RedisRoleRepository(prefix, factory),
			RedisUserRepository(prefix, factory),
			RedisVoiceStateRepository(prefix, factory),
		)
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
