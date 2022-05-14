package dev.capybaralabs.d4j.store.common

import dev.capybaralabs.d4j.store.common.repository.Repositories
import discord4j.common.store.api.layout.DataAccessor
import discord4j.discordjson.json.ChannelData
import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.GuildData
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.MessageData
import discord4j.discordjson.json.PresenceData
import discord4j.discordjson.json.RoleData
import discord4j.discordjson.json.StickerData
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CommonDataAccessor(private val repositories: Repositories) : DataAccessor {

	override fun countChannels(): Mono<Long> {
		return repositories.channels.countChannels()
			.checkpoint("countChannels")
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return repositories.channels.countChannelsInGuild(guildId)
			.checkpoint("countChannelsInGuild $guildId")
	}


	override fun countEmojis(): Mono<Long> {
		return repositories.emojis.countEmojis()
			.checkpoint("countEmojis")
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return repositories.emojis.countEmojisInGuild(guildId)
			.checkpoint("countEmojisInGuild $guildId")
	}


	override fun countGuilds(): Mono<Long> {
		return repositories.guilds.countGuilds()
			.checkpoint("countGuilds")
	}


	override fun countMembers(): Mono<Long> {
		return repositories.members.countMembers()
			.checkpoint("countMembers")
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		return repositories.members.countMembersInGuild(guildId)
			.checkpoint("countMembersInGuild $guildId")
	}

	override fun countExactMembersInGuild(guildId: Long): Mono<Long> {
		return countMembersInGuild(guildId)
			.checkpoint("countExactMembersInGuild $guildId")
	}


	override fun countMessages(): Mono<Long> {
		return repositories.messages.countMessages()
			.checkpoint("countMessages")
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		return repositories.messages.countMessagesInChannel(channelId)
			.checkpoint("countMessagesInChannel $channelId")
	}


	override fun countPresences(): Mono<Long> {
		return repositories.presences.countPresences()
			.checkpoint("countPresences")
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return repositories.presences.countPresencesInGuild(guildId)
			.checkpoint("countPresencesInGuild $guildId")
	}


	override fun countRoles(): Mono<Long> {
		return repositories.roles.countRoles()
			.checkpoint("countRoles")
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return repositories.roles.countRolesInGuild(guildId)
			.checkpoint("countRolesInGuild $guildId")
	}


	override fun countStickers(): Mono<Long> {
		return repositories.stickers.countStickers()
			.checkpoint("countStickers")
	}

	override fun countStickersInGuild(guildId: Long): Mono<Long> {
		return repositories.stickers.countStickersInGuild(guildId)
			.checkpoint("countStickersInGuild $guildId")
	}


	override fun countUsers(): Mono<Long> {
		return repositories.users.countUsers()
			.checkpoint("countUsers")
	}


	override fun countVoiceStates(): Mono<Long> {
		return repositories.voiceStates.countVoiceStates()
			.checkpoint("countVoiceStates")
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return repositories.voiceStates.countVoiceStatesInGuild(guildId)
			.checkpoint("countVoiceStatesInGuild $guildId")
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return repositories.voiceStates.countVoiceStatesInChannel(guildId, channelId)
			.checkpoint("countVoiceStatesInChannel $guildId $channelId")
	}


	override fun getChannels(): Flux<ChannelData> {
		return repositories.channels.getChannels()
			.checkpoint("getChannels")
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return repositories.channels.getChannelsInGuild(guildId)
			.checkpoint("getChannelsInGuild $guildId")
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return repositories.channels.getChannelById(channelId)
			.checkpoint("getChannelById $channelId")
	}


	override fun getEmojis(): Flux<EmojiData> {
		return repositories.emojis.getEmojis()
			.checkpoint("getEmojis")
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return repositories.emojis.getEmojisInGuild(guildId)
			.checkpoint("getEmojisInGuild $guildId")
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return repositories.emojis.getEmojiById(guildId, emojiId)
			.checkpoint("getEmojiById $guildId $emojiId")
	}


	override fun getGuilds(): Flux<GuildData> {
		return repositories.guilds.getGuilds()
			.checkpoint("getGuilds")
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return repositories.guilds.getGuildById(guildId)
			.checkpoint("getGuildById $guildId")
	}


	override fun getMembers(): Flux<MemberData> {
		return repositories.members.getMembers()
			.checkpoint("getMembers")
	}

	override fun getMembersInGuild(guildId: Long): Flux<MemberData> {
		return getExactMembersInGuild(guildId)
			.checkpoint("getMembersInGuild $guildId")
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return repositories.members.getExactMembersInGuild(guildId)
			.checkpoint("getExactMembersInGuild $guildId")
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return repositories.members.getMemberById(guildId, userId)
			.checkpoint("getMemberById $guildId $userId")
	}


	override fun getMessages(): Flux<MessageData> {
		return repositories.messages.getMessages()
			.checkpoint("getMessages")
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		return repositories.messages.getMessagesInChannel(channelId)
			.checkpoint("getMessagesInChannel $channelId")
	}

	override fun getMessageById(channelId: Long, messageId: Long): Mono<MessageData> {
		return repositories.messages.getMessageById(messageId)
			.checkpoint("getMessageById $messageId")
	}


	override fun getPresences(): Flux<PresenceData> {
		return repositories.presences.getPresences()
			.checkpoint("getPresences")
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return repositories.presences.getPresencesInGuild(guildId)
			.checkpoint("getPresencesInGuild $guildId")
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return repositories.presences.getPresenceById(guildId, userId)
			.checkpoint("getPresenceById $guildId $userId")
	}


	override fun getRoles(): Flux<RoleData> {
		return repositories.roles.getRoles()
			.checkpoint("getRoles")
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return repositories.roles.getRolesInGuild(guildId)
			.checkpoint("getRolesInGuild $guildId")
	}

	override fun getRoleById(guildId: Long, roleId: Long): Mono<RoleData> {
		return repositories.roles.getRoleById(roleId)
			.checkpoint("getRoleById $roleId")
	}


	override fun getStickers(): Flux<StickerData> {
		return repositories.stickers.getStickers()
			.checkpoint("getStickers")
	}

	override fun getStickersInGuild(guildId: Long): Flux<StickerData> {
		return repositories.stickers.getStickersInGuild(guildId)
			.checkpoint("getStickersInGuild $guildId")
	}

	override fun getStickerById(guildId: Long, stickerId: Long): Mono<StickerData> {
		return repositories.stickers.getStickerById(guildId, stickerId)
			.checkpoint("getStickerById $guildId $stickerId")
	}

	override fun getUsers(): Flux<UserData> {
		return repositories.users.getUsers()
			.checkpoint("getUsers")
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return repositories.users.getUserById(userId)
			.checkpoint("getUserById $userId")
	}


	override fun getVoiceStates(): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStates()
			.checkpoint("getVoiceStates")
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStatesInChannel(guildId, channelId)
			.checkpoint("getVoiceStatesInChannel $guildId $channelId")
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStatesInGuild(guildId)
			.checkpoint("getVoiceStatesInGuild $guildId")
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return repositories.voiceStates.getVoiceStateById(guildId, userId)
			.checkpoint("getVoiceStateById $guildId $userId")
	}
}
