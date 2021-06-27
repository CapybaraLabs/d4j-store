package dev.capybaralabs.d4j.store.postgres

import dev.capybaralabs.d4j.store.postgres.repository.Repositories
import discord4j.common.store.api.`object`.InvalidationCause
import discord4j.common.store.api.`object`.PresenceAndUserData
import discord4j.common.store.api.layout.GatewayDataUpdater
import discord4j.discordjson.Id
import discord4j.discordjson.json.ChannelData
import discord4j.discordjson.json.ClientStatusData
import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.GuildData
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.MessageData
import discord4j.discordjson.json.PartialUserData
import discord4j.discordjson.json.PresenceData
import discord4j.discordjson.json.ReactionData
import discord4j.discordjson.json.RoleData
import discord4j.discordjson.json.UserData
import discord4j.discordjson.json.VoiceStateData
import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.ChannelDelete
import discord4j.discordjson.json.gateway.ChannelUpdate
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildDelete
import discord4j.discordjson.json.gateway.GuildEmojisUpdate
import discord4j.discordjson.json.gateway.GuildMemberAdd
import discord4j.discordjson.json.gateway.GuildMemberRemove
import discord4j.discordjson.json.gateway.GuildMemberUpdate
import discord4j.discordjson.json.gateway.GuildMembersChunk
import discord4j.discordjson.json.gateway.GuildRoleCreate
import discord4j.discordjson.json.gateway.GuildRoleDelete
import discord4j.discordjson.json.gateway.GuildRoleUpdate
import discord4j.discordjson.json.gateway.GuildUpdate
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.json.gateway.MessageDelete
import discord4j.discordjson.json.gateway.MessageDeleteBulk
import discord4j.discordjson.json.gateway.MessageReactionAdd
import discord4j.discordjson.json.gateway.MessageReactionRemove
import discord4j.discordjson.json.gateway.MessageReactionRemoveAll
import discord4j.discordjson.json.gateway.MessageReactionRemoveEmoji
import discord4j.discordjson.json.gateway.MessageUpdate
import discord4j.discordjson.json.gateway.PresenceUpdate
import discord4j.discordjson.json.gateway.Ready
import discord4j.discordjson.json.gateway.UserUpdate
import discord4j.discordjson.json.gateway.VoiceStateUpdateDispatch
import discord4j.discordjson.possible.Possible
import java.util.Optional
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * TODO
 *
 *
 * The `.flatMap(PostgresqlResult::getRowsUpdated)`
 * lines are necessary because of https://github.com/pgjdbc/r2dbc-postgresql/issues/194#issuecomment-557443260
 *
 *
 * Ids have to saved as TEXT, because JSONB does not support deleting numeric array entries (easily)
 *
 *
 * Some copying going on from [discord4j.common.store.legacy.LegacyStoreLayout]
 */
internal class PostgresGatewayDataUpdater(private val repos: Repositories) : GatewayDataUpdater {

	companion object {
		private val log = LoggerFactory.getLogger(PostgresGatewayDataUpdater::class.java)
	}


	override fun onChannelCreate(shardIndex: Int, dispatch: ChannelCreate): Mono<Void> {
		val channel = dispatch.channel()
		val saveChannel = repos.channels.save(channel, shardIndex)
		val guildId = channel.guildId().toOptional()
		var addChannelToGuild = Mono.empty<Void>()
		if (guildId.isPresent) {
			addChannelToGuild = repos.guilds.getGuildById(guildId.get().asLong())
				.map { guildData ->
					GuildData.builder()
						.from(guildData)
						.addChannel(channel.id()) // TODO use deduplication?
						.build()
				}
				.flatMap { repos.guilds.save(it, shardIndex) }
		}

		return saveChannel.and(addChannelToGuild)
	}

	override fun onChannelDelete(shardIndex: Int, dispatch: ChannelDelete): Mono<ChannelData> {
		val channel = dispatch.channel()
		val channelId = channel.id().asLong()
		val guildId = channel.guildId().toOptional()

		val removeChannelFromGuild = when (guildId.isPresent) {
			true -> {
				repos.guilds.getGuildById(guildId.get().asLong())
					.map { guildData ->
						GuildData.builder()
							.from(guildData)
							.channels(guildData.channels().toList() - channel.id())
							.build()
					}
					.flatMap { repos.guilds.save(it, shardIndex) }
			}
			false -> Mono.empty()
		}

		val deleteMessagesInChannel = repos.messages.deleteByChannelId(channelId)

		val deleteChannelReturningOld = repos.channels.getChannelById(channelId)
			.flatMap { oldChannel -> repos.channels.delete(channelId).thenReturn(oldChannel) }

		return removeChannelFromGuild
			.and(deleteMessagesInChannel)
			.then(deleteChannelReturningOld)
	}

	override fun onChannelUpdate(shardIndex: Int, dispatch: ChannelUpdate): Mono<ChannelData> {
		val channel = dispatch.channel()

		val saveNew: Mono<Void> = repos.channels.save(channel, shardIndex)

		return repos.channels.getChannelById(channel.id().asLong())
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}


	override fun onGuildCreate(shardIndex: Int, dispatch: GuildCreate): Mono<Void> {
		val createData = dispatch.guild()
		val guild = GuildData.builder()
			.from(createData)
			.roles(createData.roles().map { it.id() })
			.emojis(createData.emojis().map { it.id() }.filter { it.isPresent }.map { it.get() })
			.members(createData.members().map { it.user().id() }.distinct())
			.channels(createData.channels().map { it.id() })
			.build()
		val guildId = guild.id().asLong()

		// TODO consider bulk insert methods

		val saveGuild = repos.guilds.save(guild, shardIndex)

		val saveChannels = Flux.fromIterable(createData.channels())
			.map { ChannelData.builder().from(it).guildId(guildId).build() }
			.flatMap { repos.channels.save(it, shardIndex) }

		val saveEmojis = Flux.fromIterable(createData.emojis())
			.flatMap { repos.emojis.save(guildId, it, shardIndex) }

		val saveMembers = Flux.fromIterable(createData.members())
			.flatMap { repos.members.save(guildId, it, shardIndex) }

		val savePresences = Flux.fromIterable(createData.presences())
			.flatMap { repos.presences.save(guildId, it, shardIndex) }

		// TODO why? addGuildMember does not create any presences
		val saveOfflinePresences = Flux.fromIterable(createData.members())
			.filterWhen { member ->
				repos.presences.getPresenceById(guildId, member.user().id().asLong())
					.hasElement().map { !it }
			}
			.flatMap { repos.presences.save(guildId, createOfflinePresence(it), shardIndex) }
			.then()

		val saveRoles = Flux.fromIterable(createData.roles())
			.flatMap { repos.roles.save(guildId, it, shardIndex) }

		val saveUsers = Flux.fromIterable(createData.members())
			.map { it.user() }
			.flatMap { repos.users.save(it) }

		val saveVoiceStates = Flux.fromIterable(createData.voiceStates())
			.map { VoiceStateData.builder().from(it).guildId(guildId).build() }
			.flatMap { repos.voiceStates.save(it, shardIndex) }

		return saveGuild
			.and(saveChannels)
			.and(saveEmojis)
			.and(saveMembers)
			.and(savePresences.then(saveOfflinePresences))
			.and(saveRoles)
			.and(saveUsers)
			.and(saveVoiceStates)
	}

	private fun createOfflinePresence(member: MemberData): PresenceData {
		return PresenceData.builder()
			.user(
				PartialUserData.builder()
					.id(member.user().id())
					.username(member.user().username())
					.discriminator(member.user().discriminator())
					.avatar(Possible.of(member.user().avatar()))
					.bot(member.user().bot())
					.system(member.user().system())
					.mfaEnabled(member.user().mfaEnabled())
					.locale(member.user().locale())
					.verified(member.user().verified())
					.email(member.user().email().collapse())
					.flags(member.user().flags())
					.premiumType(member.user().premiumType())
					.build()
			)
			.status("offline")
			.clientStatus(
				ClientStatusData.builder()
					.desktop(Possible.absent())
					.mobile(Possible.absent())
					.web(Possible.absent())
					.build()
			)
			.build()
	}

	override fun onGuildDelete(shardIndex: Int, dispatch: GuildDelete): Mono<GuildData> {
		val guildId = dispatch.guild().id().asLong()

		return repos.guilds.getGuildById(guildId)
			.flatMap { guild ->
				val channelIds = guild.channels().map { it.asLong() }
				val roleIds = guild.roles().map { it.asLong() }
				val emojiIds = guild.emojis().map { it.asLong() }
				val deleteChannels = repos.channels.deleteByIds(channelIds)
				val deleteRoles = repos.roles.deleteByIds(roleIds)
				val deleteEmojis = repos.emojis.deleteByIds(emojiIds)
				val deleteMembers = repos.members.deleteByGuildId(guildId)
				val deleteMessages = repos.messages.deleteByChannelIds(guild.channels().map { it.asLong() })
				// TODO delete no longer visible users
				val deleteVoiceStates = repos.voiceStates.deleteByGuildId(guildId)
				val deletePresences = repos.presences.deleteByGuildId(guildId)
				deleteChannels
					.and(deleteEmojis)
					.and(deleteMembers)
					.and(deleteMessages)
					.and(deletePresences)
					.and(deleteRoles)
					.and(deleteVoiceStates)
					.thenReturn(guild)
			}
			.flatMap { repos.guilds.delete(guildId).thenReturn(it) }
	}

	override fun onGuildEmojisUpdate(shardIndex: Int, dispatch: GuildEmojisUpdate): Mono<Set<EmojiData>> {
		val guildId = dispatch.guildId().asLong()

		// TODO granular updates, loading the object and saving it back is suboptimal?

		val updateGuild: (GuildData) -> Mono<Void> = { oldGuild: GuildData ->
			val updated = GuildData.builder()
				.from(oldGuild)
				.emojis(dispatch.emojis().mapNotNull { it.id().orElse(null) })
				.build()
			repos.guilds.save(updated, shardIndex)
		}

		val deleteEmojis: (GuildData) -> Mono<Int> = { oldGuild: GuildData ->
			// delete those emojis that are in the old guild but not the dispatch
			val toDelete = oldGuild.emojis()
				.filter { id -> dispatch.emojis().none { emoji -> emoji.id().isPresent && emoji.id().get() == id } }
				.map { it.asLong() }
			repos.emojis.deleteByIds(toDelete)
		}

		val saveEmojis = Flux.fromIterable(dispatch.emojis())
			.flatMap { repos.emojis.save(guildId, it, shardIndex) } // TODO bulk operation

		return repos.guilds.getGuildById(guildId)
			.flatMapMany { guild ->
				updateGuild.invoke(guild)
					.and(deleteEmojis.invoke(guild))
					.and(saveEmojis)
					.thenMany(
						Flux.fromIterable(guild.emojis())
							.map { it.asLong() }
							.flatMap { repos.emojis.getEmojiById(guildId, it) } // TODO bulk operation
					)
			}
			.collectList().map { it.toSet() }
	}

	override fun onGuildMemberAdd(shardIndex: Int, dispatch: GuildMemberAdd): Mono<Void> {
		val guildId = dispatch.guildId().asLong()
		val member = dispatch.member()
		val user = member.user()

		val addMemberToGuild = repos.guilds.getGuildById(guildId)
			.map {
				GuildData.builder().from(it)
					.members(
						it.members().toList() + member.user().id()
					) // TODO list operations suck, potential duplicates possible. any way we can leverage sets here?
					.memberCount(it.memberCount() + 1)
					.build()
			}
			.flatMap { repos.guilds.save(it, shardIndex) }// TODO granular update

		val saveMember = repos.members.save(guildId, member, shardIndex)
		val saveUser = repos.users.save(user)

		return addMemberToGuild
			.and(saveMember)
			.and(saveUser)
	}

	override fun onGuildMemberRemove(shardIndex: Int, dispatch: GuildMemberRemove): Mono<MemberData> {
		val guildId = dispatch.guildId().asLong()
		val userData = dispatch.user()
		val userId = userData.id().asLong()

		val removeMemberId = repos.guilds.getGuildById(guildId)
			.map {
				GuildData.builder()
					.from(it)
					// TODO list operations suck, potential duplicates possible. any way we can leverage sets here?
					.members(it.members().toList() - userData.id())
					.memberCount(it.memberCount() - 1)
					.build()
			}
			.flatMap { repos.guilds.save(it, shardIndex) }

		val getMember = repos.members.getMemberById(guildId, userId)
		val deleteMember = repos.members.deleteById(guildId, userId)
		val deletePresence = repos.presences.deleteById(guildId, userId)

		val deleteOrphanUser = repos.members.getMembersByUserId(userId) // TODO consider method that loads less data
			.filter { it.first != guildId }
			.hasElements()
			.flatMap { hasMutualServers ->
				if (hasMutualServers) Mono.empty()
				else repos.users.deleteById(userId)
			}

		val deletions = Mono.`when`(removeMemberId, deleteMember, deletePresence, deleteOrphanUser)

		return getMember
			.flatMap { deletions.thenReturn(it) }
			.switchIfEmpty(deletions.then(Mono.empty()))
	}

	override fun onGuildMembersChunk(shardIndex: Int, dispatch: GuildMembersChunk): Mono<Void> {
		val guildId = dispatch.guildId().asLong()
		val members = dispatch.members()

		// TODO consider granular update in DB
		val addMemberIds = repos.guilds.getGuildById(guildId)
			.map {
				GuildData.builder()
					.from(it)
					.members(sumDistinct(it.members(), members.map { member -> member.user().id() }))
					.build()
			}
			.flatMap { repos.guilds.save(it, shardIndex) }

		// TODO add bulk operations
		val saveMembers = Flux.fromIterable(members)
			.flatMap { repos.members.save(guildId, it, shardIndex) }

		val saveUsers = Flux.fromIterable(members)
			.flatMap { repos.users.save(it.user()) }

		// TODO why? addGuildMember does not create any presences
		val saveOfflinePresences = Flux.fromIterable(members)
			.filterWhen { member ->
				repos.presences.getPresenceById(guildId, member.user().id().asLong())
					.hasElement().map { !it }
			}
			.flatMap { repos.presences.save(guildId, createOfflinePresence(it), shardIndex) }
			.then()

		return addMemberIds
			.and(saveMembers)
			.and(saveUsers)
			.and(saveOfflinePresences)
	}

	override fun onGuildMemberUpdate(shardIndex: Int, dispatch: GuildMemberUpdate): Mono<MemberData> {
		val guildId = dispatch.guildId().asLong()
		val userId = dispatch.user().id().asLong()

		return repos.members.getMemberById(guildId, userId)
			.flatMap { oldMember ->
				val newMember = MemberData.builder()
					.from(oldMember)
					.roles(dispatch.roles().map { Id.of(it) })
					.user(dispatch.user())
					.nick(dispatch.nick())
					.joinedAt(dispatch.joinedAt())
					.premiumSince(dispatch.premiumSince())
					.pending(dispatch.pending())
					.build()
				repos.members.save(guildId, newMember, shardIndex)
					.thenReturn(oldMember)
			}
	}

	override fun onGuildRoleCreate(shardIndex: Int, dispatch: GuildRoleCreate): Mono<Void> {
		val guildId = dispatch.guildId().asLong()
		val role = dispatch.role()

		val addRoleId = repos.guilds.getGuildById(guildId)
			.map { guild ->
				GuildData.builder()
					.from(guild)
					.addRole(role.id())
					.build()
			}
			.flatMap { repos.guilds.save(it, shardIndex) }

		val saveRole = repos.roles.save(guildId, role, shardIndex)

		return addRoleId.and(saveRole)
	}

	override fun onGuildRoleDelete(shardIndex: Int, dispatch: GuildRoleDelete): Mono<RoleData> {
		val guildId = dispatch.guildId().asLong()
		val roleId = dispatch.roleId().asLong()

		val getRole = repos.roles.getRoleById(roleId)

		val removeRoleId = repos.guilds.getGuildById(guildId)
			.map { guild ->
				GuildData.builder()
					.from(guild)
					.roles(guild.roles().toList() - dispatch.roleId())
					.build()
			}
			.flatMap { repos.guilds.save(it, shardIndex) }

		val deleteRole = repos.roles.deleteById(roleId)

		val removeRoleFromMembers = repos.guilds.getGuildById(guildId)
			.flatMapMany { guild ->
				Flux.fromIterable(guild.members())
					.map { it.asLong() }
			}
			.flatMap { repos.members.getMemberById(guildId, it) }
			.filter { member -> member.roles().contains(dispatch.roleId()) }
			.map { member ->
				MemberData.builder()
					.from(member)
					.roles(member.roles().toList() - dispatch.roleId())
					.build()
			}
			.flatMap { repos.members.save(guildId, it, shardIndex) }
			.then()

		val deletions = Mono.`when`(removeRoleId, deleteRole, removeRoleFromMembers)

		return getRole
			.flatMap { deletions.thenReturn(it) }
			.switchIfEmpty(deletions.then(Mono.empty()))
	}

	override fun onGuildRoleUpdate(shardIndex: Int, dispatch: GuildRoleUpdate): Mono<RoleData?> {
		val role = dispatch.role()
		val roleId = role.id().asLong()
		val guildId = dispatch.guildId().asLong()

		val saveNew = repos.roles.save(guildId, role, shardIndex)

		return repos.roles.getRoleById(roleId)
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}

	override fun onGuildUpdate(shardIndex: Int, dispatch: GuildUpdate): Mono<GuildData> {
		val guildId = dispatch.guild().id().asLong()

		// TODO what if there is no guild saved?
		return repos.guilds.getGuildById(guildId)
			.flatMap { oldGuildData ->
				val newGuildData = GuildData.builder()
					.from(oldGuildData)
					.from(dispatch.guild())
					.roles(dispatch.guild().roles().map { it.id() })
					.emojis(
						dispatch.guild().emojis()
							.map { it.id() }
							.filter { it.isPresent }
							.map { it.get() }
					)
					.build()
				repos.guilds
					.save(newGuildData, shardIndex)
					.thenReturn(oldGuildData)
			}
	}

	override fun onShardInvalidation(shardIndex: Int, cause: InvalidationCause): Mono<Void> {
		return Flux.fromIterable(
			listOf(
				repos.channels.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it channels on shard $shardIndex") },
				repos.emojis.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it emojis on shard $shardIndex") },
				repos.guilds.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it guilds on shard $shardIndex") },
				repos.members.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it members on shard $shardIndex") },
				// TODO message cache is probably safe to be kept and should follow different kinds of cache rules
				repos.messages.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it messages on shard $shardIndex") },
				repos.presences.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it presences on shard $shardIndex") },
				repos.roles.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it roles on shard $shardIndex") },
				// TODO delete users by shard???
//				repos.users.deleteByShardIndex(shardIndex)
//					.doOnNext { log.debug("Invalidated $it users on shard $shardIndex") },
				repos.voiceStates.deleteByShardIndex(shardIndex)
					.doOnNext { log.debug("Invalidated $it voice states on shard $shardIndex") },
			)
		)
			.flatMap { it }
			.then()
	}

	override fun onMessageCreate(shardIndex: Int, dispatch: MessageCreate): Mono<Void> {
		val message = dispatch.message()
		val channelId = message.channelId().asLong()

		val saveMessage = repos.messages.save(message, shardIndex)

		val editLastMessageId = repos.channels.getChannelById(channelId)
			.map {
				ChannelData.builder()
					.from(it)
					.lastMessageId(message.id())
					.build()
			}
			.flatMap { repos.channels.save(it, shardIndex) }

		return saveMessage.and(editLastMessageId)
	}

	override fun onMessageDelete(shardIndex: Int, dispatch: MessageDelete): Mono<MessageData> {
		val messageId = dispatch.id().asLong()
		return repos.messages.getMessageById(messageId)
			.flatMap { repos.messages.delete(messageId).thenReturn(it) }
	}

	override fun onMessageDeleteBulk(shardIndex: Int, dispatch: MessageDeleteBulk): Mono<Set<MessageData>> {
		val messageIds = dispatch.ids().map { it.asLong() }

		val deleteMessages = repos.messages.deleteByIds(messageIds)

		return repos.messages.getMessagesByIds(messageIds)
			.collectList().map { it.toSet() }
			.flatMap { deleteMessages.thenReturn(it) }
	}

	override fun onMessageReactionAdd(shardIndex: Int, dispatch: MessageReactionAdd): Mono<Void> {
		val selfId = 0L // TODO include selfId as parameter

		val userId = dispatch.userId().asLong()
		val messageId = dispatch.messageId().asLong()

		// add reaction to message
		return repos.messages.getMessageById(messageId)
			.map { oldMessage ->
				val isMe = userId == selfId
				val newMessageBuilder = MessageData.builder().from(oldMessage)
				if (oldMessage.reactions().isAbsent) {
					newMessageBuilder.addReaction(
						ReactionData.builder()
							.count(1)
							.me(isMe)
							.emoji(dispatch.emoji())
							.build()
					)
				} else {
					val reactions = oldMessage.reactions().get()
					val oldExisting: ReactionData? = reactions.find { it.equalsEmoji(dispatch.emoji()) }
					if (oldExisting != null) {
						// message already has this reaction: bump 1
						val newExisting = ReactionData.builder()
							.from(oldExisting)
							.me(oldExisting.me() || isMe)
							.count(oldExisting.count() + 1)
							.build()
						val newReactions = reactions.toMutableList()
						newReactions.replaceAll {
							when {
								it.equalsEmoji(dispatch.emoji()) -> newExisting
								else -> it
							}
						}
						newMessageBuilder.reactions(newReactions)
					} else {
						// message doesn't have this reaction: create
						val reaction = ReactionData.builder()
							.emoji(dispatch.emoji())
							.me(isMe)
							.count(1)
							.build()
						newMessageBuilder.reactions(reactions.toList() + reaction)
					}
				}
				newMessageBuilder.build()
			}
			.flatMap { repos.messages.save(it, shardIndex) }
	}

	override fun onMessageReactionRemove(shardIndex: Int, dispatch: MessageReactionRemove): Mono<Void> {
		val selfId = 0L // TODO include selfId as parameter

		val userId = dispatch.userId().asLong()
		val messageId = dispatch.messageId().asLong()

		// remove reactor from message
		return repos.messages.getMessageById(messageId)
			.filter { message -> !message.reactions().isAbsent }
			.map { oldMessage: MessageData ->
				val me = userId == selfId
				val newMessageBuilder = MessageData.builder().from(oldMessage)
				val reactions = oldMessage.reactions().get()

				val existing: ReactionData? = reactions.find { it.equalsEmoji(dispatch.emoji()) }
				if (existing != null) {
					if (existing.count() - 1 == 0) {
						newMessageBuilder.reactions(reactions.toList() - existing)
					} else {
						val newExisting = ReactionData.builder()
							.from(existing)
							.count(existing.count() - 1)
							.me(!me && existing.me())
							.build()
						val newReactions = reactions.toMutableList()
						newReactions.replaceAll {
							when {
								it.equalsEmoji(dispatch.emoji()) -> newExisting
								else -> it
							}
						}
						newMessageBuilder.reactions(newReactions)
					}
				}
				newMessageBuilder.build()
			}
			.flatMap { repos.messages.save(it, shardIndex) }
	}

	override fun onMessageReactionRemoveAll(shardIndex: Int, dispatch: MessageReactionRemoveAll): Mono<Void> {
		val messageId = dispatch.messageId().asLong()

		return repos.messages.getMessageById(messageId)
			.map {
				MessageData.builder()
					.from(it)
					.reactions(Possible.absent())
					.build()
			}
			.flatMap { repos.messages.save(it, shardIndex) }
	}

	override fun onMessageReactionRemoveEmoji(shardIndex: Int, dispatch: MessageReactionRemoveEmoji): Mono<Void> {
		val messageId = dispatch.messageId().asLong()

		return repos.messages.getMessageById(messageId)
			.flatMap { oldMessage ->
				if (oldMessage.reactions().isAbsent) {
					return@flatMap Mono.empty()
				}

				val newMessageBuilder = MessageData.builder().from(oldMessage)
				val newReactions = oldMessage.reactions().get().toMutableList()

				val changed = newReactions.removeIf { reaction -> reaction.equalsEmoji(dispatch.emoji()) }
				if (!changed) {
					return@flatMap Mono.empty()
				}

				return@flatMap Mono.just(newMessageBuilder.reactions(newReactions).build())
			}
			.flatMap { repos.messages.save(it, shardIndex) }
	}

	override fun onMessageUpdate(shardIndex: Int, dispatch: MessageUpdate): Mono<MessageData> {
		val messageData = dispatch.message()
		val messageId = messageData.id().asLong()

		return repos.messages.getMessageById(messageId)
			.flatMap { oldMessageData: MessageData ->
				// TODO sort this out, check discord docs what it means if these are empty
				// updating the content and embed of the bean in the store
//				val contentChanged = !messageData.content().isAbsent &&
//					oldMessageData.content() != messageData.content().get()
//				val embedsChanged = oldMessageData.embeds() != messageData.embeds()

				val newMessageData: MessageData = MessageData.builder()
					.from(oldMessageData)
					.content(
						messageData.content().toOptional()
							.orElse(oldMessageData.content())
					)
					.embeds(messageData.embeds())
					.mentions(messageData.mentions())
					.mentionRoles(messageData.mentionRoles())
					.mentionEveryone(
						messageData.mentionEveryone().toOptional()
							.orElse(oldMessageData.mentionEveryone())
					)
					.editedTimestamp(messageData.editedTimestamp())
					.build()
				repos.messages.save(newMessageData, shardIndex)
					.thenReturn(oldMessageData)
			}
	}

	override fun onPresenceUpdate(shardIndex: Int, dispatch: PresenceUpdate): Mono<PresenceAndUserData> {
		val guildId = dispatch.guildId().asLong()
		val userData = dispatch.user()
		val userId = userData.id().asLong()
		val presenceData: PresenceData = PresenceData.builder()
			.user(dispatch.user())
			.status(dispatch.status())
			.activities(dispatch.activities())
			.clientStatus(dispatch.clientStatus())
			.build()

		val saveNew = repos.presences.save(guildId, presenceData, shardIndex)

		val savePresence = repos.presences.getPresenceById(guildId, userId)
			.flatMap { saveNew.thenReturn(it) }
			.map { Optional.of(it) }
			.switchIfEmpty(saveNew.thenReturn(Optional.empty()))

		val saveUser = repos.users.getUserById(userId)
			.flatMap { oldUserData ->
				val newUserData = UserData.builder()
					.from(oldUserData)
					.username(
						userData.username().toOptional()
							.orElse(oldUserData.username())
					)
					.discriminator(
						userData.discriminator().toOptional()
							.orElse(oldUserData.discriminator())
					)
					.avatar(if (userData.avatar().isAbsent) oldUserData.avatar() else Possible.flatOpt(userData.avatar()))
					.build()
				repos.users.save(newUserData).thenReturn(oldUserData)
			}
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())

		return Mono.zip(savePresence, saveUser) { presence, user ->
			PresenceAndUserData.of(
				presence.orElse(null),
				user.orElse(null)
			)
		}
	}

	override fun onReady(dispatch: Ready): Mono<Void> {
		val userData = dispatch.user()

		return repos.users.save(userData)
	}

	override fun onUserUpdate(shardIndex: Int, dispatch: UserUpdate): Mono<UserData> {
		val userData = dispatch.user()
		val userId = userData.id().asLong()

		val saveNew = repos.users.save(userData)

		return repos.users.getUserById(userId)
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}

	override fun onVoiceStateUpdateDispatch(shardIndex: Int, dispatch: VoiceStateUpdateDispatch): Mono<VoiceStateData> {
		val voiceStateData = dispatch.voiceState()

		val guildId = voiceStateData.guildId().get().asLong()
		val userId = voiceStateData.userId().asLong()

		val saveNewOrRemove = if (voiceStateData.channelId().isPresent) {
			repos.voiceStates.save(voiceStateData, shardIndex)
		} else {
			repos.voiceStates.deleteById(guildId, userId)
		}

		return repos.voiceStates.getVoiceStateById(guildId, userId)
			.flatMap { saveNewOrRemove.thenReturn(it) }
			.switchIfEmpty(saveNewOrRemove.then(Mono.empty()))
	}

	override fun onGuildMembersCompletion(guildId: Long): Mono<Void> {
		// TODO needs implementation
		return Mono.empty()
	}
}
