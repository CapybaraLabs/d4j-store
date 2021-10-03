package dev.capybaralabs.d4j.store.redis

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.common.repository.Repositories
import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import reactor.core.publisher.Mono

class RedisRepositories(
	override val channels: ChannelRepository,
	override val emojis: EmojiRepository,
	override val guilds: GuildRepository,
	override val members: MemberRepository,
	override val messages: MessageRepository,
	override val presences: PresenceRepository,
	override val roles: RoleRepository,
	override val users: UserRepository,
	override val voiceStates: VoiceStateRepository
) : Repositories {

	override fun deleteOrphanedUsers(shardIndex: Int): Mono<Long> {
		TODO("Not yet implemented")
	}
}
