package dev.capybaralabs.d4j.store.common.repository

import reactor.core.publisher.Mono

interface Repositories {
	val channels: ChannelRepository
	val emojis: EmojiRepository
	val guilds: GuildRepository
	val members: MemberRepository
	val messages: MessageRepository
	val presences: PresenceRepository
	val roles: RoleRepository
	val users: UserRepository
	val voiceStates: VoiceStateRepository

	fun deleteOrphanedUsers(shardIndex: Int): Mono<Long>
}
