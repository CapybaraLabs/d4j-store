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
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * TODO
 */
class CommonDataAccessor(private val repositories: Repositories) : DataAccessor {

	override fun countChannels(): Mono<Long> {
		return repositories.channels.countChannels()
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return repositories.channels.countChannelsInGuild(guildId)
	}


	override fun countEmojis(): Mono<Long> {
		return repositories.emojis.countEmojis()
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return repositories.emojis.countEmojisInGuild(guildId)
	}


	override fun countGuilds(): Mono<Long> {
		return repositories.guilds.countGuilds()
	}


	override fun countMembers(): Mono<Long> {
		return repositories.members.countMembers()
	}

	override fun countMembersInGuild(guildId: Long): Mono<Long> {
		return repositories.members.countMembersInGuild(guildId)
	}

	override fun countExactMembersInGuild(guildId: Long): Mono<Long> {
		return countMembersInGuild(guildId)
	}


	override fun countMessages(): Mono<Long> {
		return repositories.messages.countMessages()
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		return repositories.messages.countMessagesInChannel(channelId)
	}


	override fun countPresences(): Mono<Long> {
		return repositories.presences.countPresences()
	}

	override fun countPresencesInGuild(guildId: Long): Mono<Long> {
		return repositories.presences.countPresencesInGuild(guildId)
	}


	override fun countRoles(): Mono<Long> {
		return repositories.roles.countRoles()
	}

	override fun countRolesInGuild(guildId: Long): Mono<Long> {
		return repositories.roles.countRolesInGuild(guildId)
	}


	override fun countUsers(): Mono<Long> {
		return repositories.users.countUsers()
	}


	override fun countVoiceStates(): Mono<Long> {
		return repositories.voiceStates.countVoiceStates()
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return repositories.voiceStates.countVoiceStatesInGuild(guildId)
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return repositories.voiceStates.countVoiceStatesInChannel(guildId, channelId)
	}


	override fun getChannels(): Flux<ChannelData> {
		return repositories.channels.getChannels()
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return repositories.channels.getChannelsInGuild(guildId)
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return repositories.channels.getChannelById(channelId)
	}


	override fun getEmojis(): Flux<EmojiData> {
		return repositories.emojis.getEmojis()
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return repositories.emojis.getEmojisInGuild(guildId)
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return repositories.emojis.getEmojiById(guildId, emojiId)
	}


	override fun getGuilds(): Flux<GuildData> {
		return repositories.guilds.getGuilds()
	}

	override fun getGuildById(guildId: Long): Mono<GuildData> {
		return repositories.guilds.getGuildById(guildId)
	}


	override fun getMembers(): Flux<MemberData> {
		return repositories.members.getMembers()
	}

	override fun getMembersInGuild(guildId: Long): Flux<MemberData> {
		return getExactMembersInGuild(guildId)
	}

	override fun getExactMembersInGuild(guildId: Long): Flux<MemberData> {
		return repositories.members.getExactMembersInGuild(guildId)
	}

	override fun getMemberById(guildId: Long, userId: Long): Mono<MemberData> {
		return repositories.members.getMemberById(guildId, userId)
	}


	override fun getMessages(): Flux<MessageData> {
		return repositories.messages.getMessages()
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		return repositories.messages.getMessagesInChannel(channelId)
	}

	override fun getMessageById(channelId: Long, messageId: Long): Mono<MessageData> {
		return repositories.messages.getMessageById(messageId)
	}


	override fun getPresences(): Flux<PresenceData> {
		return repositories.presences.getPresences()
	}

	override fun getPresencesInGuild(guildId: Long): Flux<PresenceData> {
		return repositories.presences.getPresencesInGuild(guildId)
	}

	override fun getPresenceById(guildId: Long, userId: Long): Mono<PresenceData> {
		return repositories.presences.getPresenceById(guildId, userId)
	}


	override fun getRoles(): Flux<RoleData> {
		return repositories.roles.getRoles()
	}

	override fun getRolesInGuild(guildId: Long): Flux<RoleData> {
		return repositories.roles.getRolesInGuild(guildId)
	}

	override fun getRoleById(guildId: Long, roleId: Long): Mono<RoleData> {
		return repositories.roles.getRoleById(roleId)
	}


	override fun getUsers(): Flux<UserData> {
		return repositories.users.getUsers()
	}

	override fun getUserById(userId: Long): Mono<UserData> {
		return repositories.users.getUserById(userId)
	}


	override fun getVoiceStates(): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStates()
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStatesInChannel(guildId, channelId)
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return repositories.voiceStates.getVoiceStatesInGuild(guildId)
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return repositories.voiceStates.getVoiceStateById(guildId, userId)
	}
}
