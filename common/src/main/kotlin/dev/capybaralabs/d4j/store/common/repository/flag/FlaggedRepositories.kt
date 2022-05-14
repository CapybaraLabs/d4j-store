package dev.capybaralabs.d4j.store.common.repository.flag

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.common.repository.GuildRepository
import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import dev.capybaralabs.d4j.store.common.repository.Repositories
import dev.capybaralabs.d4j.store.common.repository.RoleRepository
import dev.capybaralabs.d4j.store.common.repository.StickerRepository
import dev.capybaralabs.d4j.store.common.repository.UserRepository
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopChannelRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopEmojiRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopGuildRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopMemberRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopMessageRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopPresenceRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopRoleRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopStickerRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopUserRepository
import dev.capybaralabs.d4j.store.common.repository.noop.NoopVoiceStateRepository
import java.util.EnumSet
import reactor.core.publisher.Mono

class FlaggedRepositories(private val storeFlags: EnumSet<StoreFlag>, private val delegate: Repositories) : Repositories {

	private val noopChannels = NoopChannelRepository()
	private val noopEmojis = NoopEmojiRepository()
	private val noopGuilds = NoopGuildRepository()
	private val noopMembers = NoopMemberRepository()
	private val minimalUserMembers = MinimalUserMemberRepository(delegate.members)
	private val noopMessages = NoopMessageRepository()
	private val noopPresences = NoopPresenceRepository()
	private val minimalUserPresences = MinimalUserPresenceRepository(delegate.presences)
	private val noopRoles = NoopRoleRepository()
	private val noopStickers = NoopStickerRepository()
	private val noopUsers = NoopUserRepository()
	private val noopVoiceStates = NoopVoiceStateRepository()


	override val channels: ChannelRepository
		get() = if (storeFlags.contains(StoreFlag.CHANNEL)) delegate.channels else noopChannels
	override val emojis: EmojiRepository
		get() = if (storeFlags.contains(StoreFlag.EMOJI)) delegate.emojis else noopEmojis
	override val guilds: GuildRepository
		get() = if (storeFlags.contains(StoreFlag.GUILD)) delegate.guilds else noopGuilds
	override val members: MemberRepository
		get() = if (storeFlags.contains(StoreFlag.MEMBER))
			if (storeFlags.contains(StoreFlag.USER))
				delegate.members
			else
				minimalUserMembers
		else
			noopMembers
	override val messages: MessageRepository
		get() = if (storeFlags.contains(StoreFlag.MESSAGE)) delegate.messages else noopMessages
	override val presences: PresenceRepository
		get() = if (storeFlags.contains(StoreFlag.PRESENCE))
			if (storeFlags.contains(StoreFlag.USER))
				delegate.presences
			else
				minimalUserPresences
		else
			noopPresences
	override val roles: RoleRepository
		get() = if (storeFlags.contains(StoreFlag.ROLE)) delegate.roles else noopRoles
	override val stickers: StickerRepository
		get() = if (storeFlags.contains(StoreFlag.STICKER)) delegate.stickers else noopStickers
	override val users: UserRepository
		get() = if (storeFlags.contains(StoreFlag.USER)) delegate.users else noopUsers
	override val voiceStates: VoiceStateRepository
		get() = if (storeFlags.contains(StoreFlag.VOICE_STATE)) delegate.voiceStates else noopVoiceStates


	override fun deleteOrphanedUsers(shardId: Int): Mono<Long> {
		return delegate.deleteOrphanedUsers(shardId)
	}
}
