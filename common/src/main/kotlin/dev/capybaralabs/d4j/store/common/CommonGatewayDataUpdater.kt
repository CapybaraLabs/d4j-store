package dev.capybaralabs.d4j.store.common

import dev.capybaralabs.d4j.store.common.repository.Repositories
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
import java.time.Duration
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import reactor.util.function.Tuples


/**
 * Some copying going on from [discord4j.common.store.legacy.LegacyStoreLayout]
 */
class CommonGatewayDataUpdater(private val repos: Repositories) : GatewayDataUpdater {

	companion object {
		private val log = LoggerFactory.getLogger(CommonGatewayDataUpdater::class.java)
	}

	private val selfUser = AtomicReference<UserData?>()


	override fun onChannelCreate(shardId: Int, dispatch: ChannelCreate): Mono<Void> {
		val channel = dispatch.channel()
		val saveChannel = repos.channels.save(channel, shardId)
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
				.flatMap { repos.guilds.save(it, shardId) }
		}

		return saveChannel.and(addChannelToGuild)
	}

	override fun onChannelDelete(shardId: Int, dispatch: ChannelDelete): Mono<ChannelData> {
		val channel = dispatch.channel()
		val channelId = channel.id().asLong()
		val guildId: Id? = channel.guildId().toOptional().orElse(null)

		val removeChannelFromGuild = when (guildId != null) {
			true -> {
				repos.guilds.getGuildById(guildId.asLong())
					.map { guildData ->
						GuildData.builder()
							.from(guildData)
							.channels(guildData.channels().toList() - channel.id())
							.build()
					}
					.flatMap { repos.guilds.save(it, shardId) }
			}
			false -> Mono.empty()
		}

		val deleteMessagesInChannel = repos.messages.deleteByChannelId(channelId)

		val deleteChannelReturningOld = repos.channels.getChannelById(channelId)
			.flatMap { oldChannel -> repos.channels.delete(channelId, guildId?.asLong()).thenReturn(oldChannel) }

		// TODO delete voice states in channel?

		return removeChannelFromGuild
			.and(deleteMessagesInChannel)
			.then(deleteChannelReturningOld)
	}

	override fun onChannelUpdate(shardId: Int, dispatch: ChannelUpdate): Mono<ChannelData> {
		val channel = dispatch.channel()

		val saveNew: Mono<Void> = repos.channels.save(channel, shardId)

		return repos.channels.getChannelById(channel.id().asLong())
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}


	private val batchers = ConcurrentHashMap<Int, Sinks.Many<Tuple2<GuildCreate, Sinks.Empty<Void>>>>()
	private val batcherTimer = Schedulers.newBoundedElastic(100, Integer.MAX_VALUE, "batcher-timer")

	override fun onGuildCreate(shardId: Int, dispatch: GuildCreate): Mono<Void> {
		val completeSink = Sinks.empty<Void>()
		batchers
			.computeIfAbsent(shardId) { createBatcher(it) }
			.emitNext(Tuples.of(dispatch, completeSink), retryAlways)
		return completeSink.asMono()
	}

	private val batchSize = 256 // TODO tune
	private val batchPeriod = Duration.ofSeconds(1) // TODO tune

	private val retryAlways: Sinks.EmitFailureHandler = Sinks.EmitFailureHandler { _, _ ->
		LockSupport.parkNanos(10)
		true
	}

	private fun createBatcher(shardId: Int): Sinks.Many<Tuple2<GuildCreate, Sinks.Empty<Void>>> {
		val batcher = Sinks.many().unicast()
			.onBackpressureBuffer<Tuple2<GuildCreate, Sinks.Empty<Void>>>()

		batcher.asFlux()
			.bufferTimeout(batchSize, batchPeriod, batcherTimer)
			.flatMap { batch ->
				val sinks = batch.map { it.t2 }
				log.debug("Batch on shard $shardId executing with ${batch.size} elements")
				onGuildCreateBatch(shardId, batch)
					.doOnSuccess { sinks.forEach { it.emitEmpty(retryAlways) } }
					.doOnError { error -> sinks.forEach { it.emitError(error, retryAlways) } }
					.onErrorResume { Mono.empty() }
			}
			.subscribe() // TODO dispose?

		return batcher
	}

	private fun onGuildCreateBatch(shardId: Int, batch: List<Tuple2<GuildCreate, Sinks.Empty<Void>>>): Mono<Void> {
		val guildCreateDatas = batch.map { it.t1.guild() }
		val guildDatas = guildCreateDatas
			.map { guildCreateData ->
				GuildData.builder()
					.from(guildCreateData)
					.roles(guildCreateData.roles().map { it.id() })
					.emojis(guildCreateData.emojis().map { it.id() }.filter { it.isPresent }.map { it.get() })
					.members(guildCreateData.members().map { it.user().id() }.distinct())
					.channels(guildCreateData.channels().map { it.id() })
					.build()
			}

		val saveGuilds = repos.guilds.saveAll(guildDatas, shardId)

		val saveChannels = guildCreateDatas.flatMap { guildCreateData ->
			guildCreateData.channels().map { channelData ->
				ChannelData.builder()
					.from(channelData)
					.guildId(guildCreateData.id().asLong())
					.build()
			}
		}.let { repos.channels.saveAll(it, shardId) }

		val saveEmojis = guildCreateDatas
			.associateBy({ it.id().asLong() }, { it.emojis() })
			.let { repos.emojis.saveAll(it, shardId) }

		val saveMembers = guildCreateDatas
			.associateBy({ it.id().asLong() }, { it.members() })
			.let { repos.members.saveAll(it, shardId) }

		val savePresences = guildCreateDatas
			.associateBy({ it.id().asLong() }, { it.presences() })
			.let { repos.presences.saveAll(it, shardId) }

		// TODO why? addGuildMember does not create any presences
//		val saveOfflinePresences = guildCreateDatas
//			.associateBy({ it.id().asLong() }) { it.members() }
//			.map { (guildId, members) ->
//				Flux.fromIterable(members)
//					.filterWhen { member ->
//						repos.presences.getPresenceById(guildId, member.user().id().asLong())
//							.hasElement().map { !it }
//					}
//					.map { createOfflinePresence(it) }
//					.collectList()
//					.map { offlinePresences -> Pair(guildId, offlinePresences) }
//			}
//			.let { Flux.fromIterable(it) }
//			.flatMap { it }
//			.collectList()
//			.map { it.toMap() }
//			.flatMap { repos.presences.saveAll(it, shardId) }

		val saveRoles = guildCreateDatas
			.associateBy({ it.id().asLong() }, { it.roles() })
			.let { repos.roles.saveAll(it, shardId) }
		val saveUsers = guildCreateDatas.flatMap { it.members() }.map { it.user() }.let { repos.users.saveAll(it) }

		val saveVoiceStates = guildCreateDatas
			.flatMap { guild ->
				guild.voiceStates()
					.map { VoiceStateData.builder().from(it).guildId(guild.id().asLong()).build() }
			}
			.let { repos.voiceStates.saveAll(it, shardId) }

		return saveGuilds
			.and(saveChannels)
			.and(saveEmojis)
			.and(saveMembers)
			.and(savePresences/*.then(saveOfflinePresences)*/)
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

	override fun onGuildDelete(shardId: Int, dispatch: GuildDelete): Mono<GuildData> {
		val guildId = dispatch.guild().id().asLong()

		return repos.guilds.getGuildById(guildId)
			.flatMap { guild ->
				val deleteChannels = repos.channels.deleteByGuildId(guildId)
				val deleteRoles = repos.roles.deleteByGuildId(guildId)
				val deleteEmojis = repos.emojis.deleteByGuildId(guildId)
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

	override fun onGuildEmojisUpdate(shardId: Int, dispatch: GuildEmojisUpdate): Mono<Set<EmojiData>> {
		val guildId = dispatch.guildId().asLong()

		// TODO granular updates, loading the object and saving it back is suboptimal?

		val updateGuild: (GuildData) -> Mono<Void> = { oldGuild: GuildData ->
			val updated = GuildData.builder()
				.from(oldGuild)
				.emojis(dispatch.emojis().mapNotNull { it.id().orElse(null) })
				.build()
			repos.guilds.save(updated, shardId)
		}

		val deleteEmojis: (GuildData) -> Mono<Long> = { oldGuild: GuildData ->
			// delete those emojis that are in the old guild but not the dispatch
			val toDelete = oldGuild.emojis()
				.filter { id -> dispatch.emojis().none { emoji -> emoji.id().isPresent && emoji.id().get() == id } }
				.map { it.asLong() }
			repos.emojis.deleteByIds(toDelete, guildId)
		}

		val saveEmojis = dispatch.emojis()
			.let { repos.emojis.saveAll(mapOf(Pair(guildId, it)), shardId) }

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

	override fun onGuildMemberAdd(shardId: Int, dispatch: GuildMemberAdd): Mono<Void> {
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
			.flatMap { repos.guilds.save(it, shardId) }// TODO granular update

		val saveMember = repos.members.save(guildId, member, shardId)
		val saveUser = repos.users.save(user)

		return addMemberToGuild
			.and(saveMember)
			.and(saveUser)
	}

	override fun onGuildMemberRemove(shardId: Int, dispatch: GuildMemberRemove): Mono<MemberData> {
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
			.flatMap { repos.guilds.save(it, shardId) }

		val getMember = repos.members.getMemberById(guildId, userId)
		val deleteMember = repos.members.deleteById(guildId, userId)
		val deletePresence = repos.presences.deleteById(guildId, userId)

		// TODO: this unexpectedly deletes the user when the MemberRepo is noop
		val deleteOrphanUser = repos.members.getMembersByUserId(userId)
			.filter { it.first != guildId }
			.hasElements() // short circuit keeps this a low impact call
			.flatMap { hasMutualServers ->
				if (hasMutualServers) Mono.empty()
				else repos.users.deleteById(userId)
			}

		val deletions = Mono.`when`(removeMemberId, deleteMember, deletePresence, deleteOrphanUser)

		return getMember
			.flatMap { deletions.thenReturn(it) }
			.switchIfEmpty(deletions.then(Mono.empty()))
	}

	override fun onGuildMembersChunk(shardId: Int, dispatch: GuildMembersChunk): Mono<Void> {
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
			.flatMap { repos.guilds.save(it, shardId) }

		val saveMembers = members.let { repos.members.saveAll(mapOf(Pair(guildId, it)), shardId) }
		val saveUsers = members.map { it.user() }.let { repos.users.saveAll(it) }

		// TODO why? addGuildMember does not create any presences
		val saveOfflinePresences = Flux.fromIterable(members)
			.filterWhen { member ->
				repos.presences.getPresenceById(guildId, member.user().id().asLong())
					.hasElement().map { !it }
			}
			// TODO add bulk operations
			.flatMap { repos.presences.save(guildId, createOfflinePresence(it), shardId) }
			.then()

		return addMemberIds
			.and(saveMembers)
			.and(saveUsers)
			.and(saveOfflinePresences)
	}

	override fun onGuildMemberUpdate(shardId: Int, dispatch: GuildMemberUpdate): Mono<MemberData> {
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
				repos.members.save(guildId, newMember, shardId)
					.thenReturn(oldMember)
			}
	}

	override fun onGuildRoleCreate(shardId: Int, dispatch: GuildRoleCreate): Mono<Void> {
		val guildId = dispatch.guildId().asLong()
		val role = dispatch.role()

		val addRoleId = repos.guilds.getGuildById(guildId)
			.map { guild ->
				GuildData.builder()
					.from(guild)
					.addRole(role.id())
					.build()
			}
			.flatMap { repos.guilds.save(it, shardId) }

		val saveRole = repos.roles.save(guildId, role, shardId)

		return addRoleId.and(saveRole)
	}

	override fun onGuildRoleDelete(shardId: Int, dispatch: GuildRoleDelete): Mono<RoleData> {
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
			.flatMap { repos.guilds.save(it, shardId) }

		val deleteRole = repos.roles.deleteById(roleId, guildId)

		val removeRoleFromMembers = repos.guilds.getGuildById(guildId)
			.flatMapMany { guild ->
				Flux.fromIterable(guild.members())
					.map { it.asLong() }
			}
			// TODO add bulk operation / granular update
			.flatMap { repos.members.getMemberById(guildId, it) }
			.filter { member -> member.roles().contains(dispatch.roleId()) }
			.map { member ->
				MemberData.builder()
					.from(member)
					.roles(member.roles().toList() - dispatch.roleId())
					.build()
			}
			// TODO add bulk operation
			.flatMap { repos.members.save(guildId, it, shardId) }
			.then()

		val deletions = Mono.`when`(removeRoleId, deleteRole, removeRoleFromMembers)

		return getRole
			.flatMap { deletions.thenReturn(it) }
			.switchIfEmpty(deletions.then(Mono.empty()))
	}

	override fun onGuildRoleUpdate(shardId: Int, dispatch: GuildRoleUpdate): Mono<RoleData?> {
		val role = dispatch.role()
		val roleId = role.id().asLong()
		val guildId = dispatch.guildId().asLong()

		val saveNew = repos.roles.save(guildId, role, shardId)

		return repos.roles.getRoleById(roleId)
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}

	override fun onGuildUpdate(shardId: Int, dispatch: GuildUpdate): Mono<GuildData> {
		val guildId = dispatch.guild().id().asLong()

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
					.save(newGuildData, shardId)
					.thenReturn(oldGuildData)
			}
	}

	override fun onShardInvalidation(shardId: Int, cause: InvalidationCause): Mono<Void> {
		val deleteChannels = repos.channels.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it channels on shard $shardId") }
		val deleteEmojis = repos.emojis.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it emojis on shard $shardId") }
		val deleteGuilds = repos.guilds.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it guilds on shard $shardId") }
		val deleteMembers = repos.members.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it members on shard $shardId") }
		val deleteOrphanedUsers = repos.deleteOrphanedUsers(shardId)
		// TODO message cache is probably safe to be kept and should follow different kinds of cache rules
		val deleteMessages = repos.messages.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it messages on shard $shardId") }
		val deletePresenses = repos.presences.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it presences on shard $shardId") }
		val deleteRoles = repos.roles.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it roles on shard $shardId") }
		val deleteVoiceStates = repos.voiceStates.deleteByShardId(shardId)
			.doOnNext { log.debug("Invalidated $it voice states on shard $shardId") }


		return Mono.`when`(
			deleteChannels,
			deleteEmojis,
			deleteGuilds,
			deleteOrphanedUsers.then(deleteMembers),
			deleteMessages,
			deletePresenses,
			deleteRoles,
			deleteVoiceStates
		)
	}

	override fun onMessageCreate(shardId: Int, dispatch: MessageCreate): Mono<Void> {
		val message = dispatch.message()
		val channelId = message.channelId().asLong()

		val saveMessage = repos.messages.save(message, shardId)

		val editLastMessageId = repos.channels.getChannelById(channelId)
			.map {
				ChannelData.builder()
					.from(it)
					.lastMessageId(message.id())
					.build()
			}
			.flatMap { repos.channels.save(it, shardId) }

		return saveMessage.and(editLastMessageId)
	}

	override fun onMessageDelete(shardId: Int, dispatch: MessageDelete): Mono<MessageData> {
		val messageId = dispatch.id().asLong()
		val channelId = dispatch.channelId().asLong()
		return repos.messages.getMessageById(messageId)
			.flatMap { repos.messages.delete(messageId, channelId).thenReturn(it) }
	}

	override fun onMessageDeleteBulk(shardId: Int, dispatch: MessageDeleteBulk): Mono<Set<MessageData>> {
		val messageIds = dispatch.ids().map { it.asLong() }
		val channelId = dispatch.channelId().asLong()

		val deleteMessages = repos.messages.deleteByIds(messageIds, channelId)

		return repos.messages.getMessagesByIds(messageIds)
			.collectList().map { it.toSet() }
			.flatMap { deleteMessages.thenReturn(it) }
	}

	override fun onMessageReactionAdd(shardId: Int, dispatch: MessageReactionAdd): Mono<Void> {
		val userId = dispatch.userId().asLong()
		val messageId = dispatch.messageId().asLong()

		// add reaction to message
		return repos.messages.getMessageById(messageId)
			.map { oldMessage ->
				val isSelf = userId == getSelfId()
				val newMessageBuilder = MessageData.builder().from(oldMessage)
				if (oldMessage.reactions().isAbsent) {
					newMessageBuilder.addReaction(
						ReactionData.builder()
							.count(1)
							.me(isSelf)
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
							.me(oldExisting.me() || isSelf)
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
							.me(isSelf)
							.count(1)
							.build()
						newMessageBuilder.reactions(reactions.toList() + reaction)
					}
				}
				newMessageBuilder.build()
			}
			.flatMap { repos.messages.save(it, shardId) }
	}

	override fun onMessageReactionRemove(shardId: Int, dispatch: MessageReactionRemove): Mono<Void> {
		val userId = dispatch.userId().asLong()
		val messageId = dispatch.messageId().asLong()

		// remove reactor from message
		return repos.messages.getMessageById(messageId)
			.filter { message -> !message.reactions().isAbsent }
			.flatMap { oldMessage: MessageData ->
				val isSelf = userId == getSelfId()
				val newMessageBuilder = MessageData.builder().from(oldMessage)
				val reactions = oldMessage.reactions().get()

				val existing: ReactionData? = reactions.find { it.equalsEmoji(dispatch.emoji()) }
				if (existing != null) {
					val newReactions: List<ReactionData>
					if (existing.count() - 1 == 0) {
						newReactions = reactions.toList() - existing
					} else {
						val newExisting = ReactionData.builder()
							.from(existing)
							.count(existing.count() - 1)
							.me(!isSelf && existing.me())
							.build()
						newReactions = reactions.toMutableList()
						newReactions.replaceAll {
							when {
								it.equalsEmoji(dispatch.emoji()) -> newExisting
								else -> it
							}
						}
					}
					if (newReactions.isEmpty()) {
						newMessageBuilder.reactions(Possible.absent())
					} else {
						newMessageBuilder.reactions(newReactions)
					}
					return@flatMap Mono.just(newMessageBuilder.build())
				}
				Mono.empty() // avoid writing when there are no changes
			}
			.flatMap { repos.messages.save(it, shardId) }
	}

	override fun onMessageReactionRemoveAll(shardId: Int, dispatch: MessageReactionRemoveAll): Mono<Void> {
		val messageId = dispatch.messageId().asLong()

		return repos.messages.getMessageById(messageId)
			.map {
				MessageData.builder()
					.from(it)
					.reactions(Possible.absent())
					.build()
			}
			.flatMap { repos.messages.save(it, shardId) }
	}

	override fun onMessageReactionRemoveEmoji(shardId: Int, dispatch: MessageReactionRemoveEmoji): Mono<Void> {
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

				if (newReactions.isEmpty()) {
					newMessageBuilder.reactions(Possible.absent())
				} else {
					newMessageBuilder.reactions(newReactions)
				}
				return@flatMap Mono.just(newMessageBuilder.build())
			}
			.flatMap { repos.messages.save(it, shardId) }
	}

	override fun onMessageUpdate(shardId: Int, dispatch: MessageUpdate): Mono<MessageData> {
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
				repos.messages.save(newMessageData, shardId)
					.thenReturn(oldMessageData)
			}
	}

	override fun onPresenceUpdate(shardId: Int, dispatch: PresenceUpdate): Mono<PresenceAndUserData> {
		val guildId = dispatch.guildId().asLong()
		val userData = dispatch.user()
		val userId = userData.id().asLong()
		val presenceData: PresenceData = PresenceData.builder()
			.user(dispatch.user())
			.status(dispatch.status())
			.activities(dispatch.activities())
			.clientStatus(dispatch.clientStatus())
			.build()

		val saveNew = repos.presences.save(guildId, presenceData, shardId)

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
		this.selfUser.set(userData)
		return repos.users.save(userData)
	}

	override fun onUserUpdate(shardId: Int, dispatch: UserUpdate): Mono<UserData> {
		val userData = dispatch.user()
		val userId = userData.id().asLong()

		val saveNew = repos.users.save(userData)

		return repos.users.getUserById(userId)
			.flatMap { saveNew.thenReturn(it) }
			.switchIfEmpty(saveNew.then(Mono.empty()))
	}

	override fun onVoiceStateUpdateDispatch(shardId: Int, dispatch: VoiceStateUpdateDispatch): Mono<VoiceStateData> {
		val voiceStateData = dispatch.voiceState()

		val guildId = voiceStateData.guildId().get().asLong()
		val userId = voiceStateData.userId().asLong()

		val saveNewOrRemove = if (voiceStateData.channelId().isPresent) {
			repos.voiceStates.save(voiceStateData, shardId)
		} else {
			repos.voiceStates.deleteById(guildId, userId)
		}

		return repos.voiceStates.getVoiceStateById(guildId, userId)
			.flatMap { saveNewOrRemove.thenReturn(it) }
			.switchIfEmpty(saveNewOrRemove.then(Mono.empty()))
	}

	override fun onGuildMembersCompletion(guildId: Long): Mono<Void> {
		return Mono.empty()
	}

	private fun getSelfId(): Long {
		return this.selfUser.get()?.id()?.asLong()
			?: throw IllegalStateException("No self user present, did we not receive onReady?")
	}
}
