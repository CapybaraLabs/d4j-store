package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.Repositories
import dev.capybaralabs.d4j.store.redis.repository.RedisChannelRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisEmojiRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisGuildRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisMemberRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisMessageRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisPresenceRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisRoleRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisUserRepository
import dev.capybaralabs.d4j.store.redis.repository.RedisVoiceStateRepository
import reactor.core.publisher.Mono

internal class RedisRepositories(
	override val channels: RedisChannelRepository,
	override val emojis: RedisEmojiRepository,
	override val guilds: RedisGuildRepository,
	override val members: RedisMemberRepository,
	override val messages: RedisMessageRepository,
	override val presences: RedisPresenceRepository,
	override val roles: RedisRoleRepository,
	override val users: RedisUserRepository,
	override val voiceStates: RedisVoiceStateRepository
) : Repositories {

	override fun deleteOrphanedUsers(shardId: Int): Mono<Long> {

		val fetchOnThisShard = members.getUserIdsOnShard(shardId).collectSet()
		// TODO think about using collections inside of redis, pulling them out seems expensive
		val fetchOnOtherShards = members.getUserIdsOnOtherShards(shardId).collectSet()

		return Mono.zip(fetchOnThisShard, fetchOnOtherShards)
			.map { it.t1 - it.t2 }
			.flatMap { orphaned -> users.deleteByIds(orphaned) }
	}
}
