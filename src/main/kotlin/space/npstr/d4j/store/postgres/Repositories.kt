package space.npstr.d4j.store.postgres

class Repositories internal constructor(
	internal val channels: ChannelRepository,
	internal val emojis: EmojiRepository,
	internal val guilds: GuildRepository,
	internal val members: MemberRepository,
	internal val messages: MessageRepository,
	internal val presences: PresenceRepository,
	internal val roles: RoleRepository,
	internal val users: UserRepository,
	internal val voiceStates: VoiceStateRepository,
)
