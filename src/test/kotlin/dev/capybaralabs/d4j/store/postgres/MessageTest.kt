package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.ImmutableReactionData
import discord4j.discordjson.json.PartialApplicationInfoData
import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.json.gateway.MessageDelete
import discord4j.discordjson.json.gateway.MessageDeleteBulk
import discord4j.discordjson.json.gateway.MessageReactionAdd
import discord4j.discordjson.json.gateway.MessageUpdate
import discord4j.discordjson.json.gateway.Ready
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MessageTest {

	@Test
	fun countMessages() {
		assertThat(accessor.countMessages().blockOptional()).isPresent
	}


	@Test
	fun onMessageCreate_createMessage() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()

		createMessage(channelId, messageId, authorId)

		val message = accessor.getMessageById(channelId, messageId).block()!!
		assertThat(message.id().asLong()).isEqualTo(messageId)
		assertThat(message.channelId().asLong()).isEqualTo(channelId)
		assertThat(message.author().id().asLong()).isEqualTo(authorId)
		assertThat(message.content()).isEqualTo("🖖")

		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isEqualTo(1)
	}

	@Test
	fun onMessageCreate_updateLastMessageIdOfChannel() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		createChannel(channelId)

		createMessage(channelId, messageId, authorId)

		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.lastMessageId().get().get().asLong()).isEqualTo(messageId)
	}

	@Test
	fun onMessageDelete_deleteMessage() {
		val channelId = generateUniqueSnowflakeId()
		val messageIdA = generateUniqueSnowflakeId()
		val messageIdB = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()

		createMessage(channelId, messageIdA, authorId)
		createMessage(channelId, messageIdB, authorId)

		val messageDelete = MessageDelete.builder()
			.channelId(channelId)
			.id(messageIdA)
			.build()
		updater.onMessageDelete(0, messageDelete).block()

		assertThat(accessor.getMessageById(channelId, messageIdA).block()).isNull()
		assertThat(accessor.getMessageById(channelId, messageIdB).block()).isNotNull
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isOne
	}

	@Test
	fun onMessageDeleteBulk_deleteMessages() {
		val channelId = generateUniqueSnowflakeId()
		val messageIdA = generateUniqueSnowflakeId()
		val messageIdB = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()

		createMessage(channelId, messageIdA, authorId)
		createMessage(channelId, messageIdB, authorId)

		val messageDeleteBulk = MessageDeleteBulk.builder()
			.channelId(channelId)
			.addAllIds(listOf(messageIdA, messageIdB))
			.build()
		updater.onMessageDeleteBulk(0, messageDeleteBulk).block()

		assertThat(accessor.getMessageById(channelId, messageIdA).block()).isNull()
		assertThat(accessor.getMessageById(channelId, messageIdB).block()).isNull()
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isZero
	}

	@Test
	fun onMessageReactionAdd_singleReaction() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId, authorId)

		addReaction(guildId, channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun onMessageReactionAdd_handleUnicodeEmoji() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId, authorId)

		addReaction(guildId, channelId, messageId, unicodeEmoji("👌").build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().name().get() == "👌" && it.count() == 1 && !it.me() }
			}
	}


	@Test
	fun onMessageReactionAdd_multipleReactors() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiId)

		addReaction(guildId, channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 2 && !it.me() }
			}
	}

	@Test
	fun onMessageReactionAdd_multipleReactions() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiIdA)

		addReaction(guildId, channelId, messageId, emoji(emojiIdA).build(), userId)
		addReaction(guildId, channelId, messageId, emoji(emojiIdB).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(2)
					.anyMatch { it.emoji().id().get().asLong() == emojiIdA && it.count() == 2 && !it.me() }
					.anyMatch { it.emoji().id().get().asLong() == emojiIdB && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun onMessageReactionAdd_me() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiId)

		addReaction(guildId, channelId, messageId, emoji(emojiId).build(), selfId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 2 && it.me() }
			}
	}

	@Test
	fun givenMessageNotExists_onMessageReactionAdd_doNothing() {
		val selfId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)

		addReaction(guildId, channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}

	// TODO onMessageReactionRemove

	// TODO onMessageReactionRemoveAll

	// TODO onMessageReactionRemoveEmoji

	// TODO more onMessageUpdate tests for other properties
	@Test
	fun onMessageUpdate_updateContent() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()

		createMessage(channelId, messageId, authorId)

		val messageUpdate = MessageUpdate.builder()
			.message(
				partialMessage(channelId, messageId)
					.content("Gagh")
					.build()
			)
			.build()
		updater.onMessageUpdate(0, messageUpdate).block()

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.content() == "Gagh" }
	}

	@Test
	fun givenUncachedMessage_onMessageUpdate_doNothing() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()

		val messageUpdate = MessageUpdate.builder()
			.message(
				partialMessage(channelId, messageId)
					.content("Gagh")
					.build()
			)
			.build()
		updater.onMessageUpdate(0, messageUpdate).block()

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}

	private fun createMessage(channelId: Long, messageId: Long, authorId: Long) {
		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, authorId).build())
			.build()

		updater.onMessageCreate(0, messageCreate).block()
	}

	private fun createChannel(channelId: Long) {
		val channelCreate = ChannelCreate.builder()
			.channel(channel(channelId).build())
			.build()

		updater.onChannelCreate(0, channelCreate).block()
	}

	private fun createMessageWithReaction(
		channelId: Long,
		messageId: Long,
		authorId: Long,
		emojiId: Long,
	) {
		val messageCreate = MessageCreate.builder()
			.message(
				message(channelId, messageId, authorId)
					.addReaction(
						ImmutableReactionData.builder()
							.emoji(emoji(emojiId).build())
							.count(1)
							.me(false)
							.build()
					)
					.build()
			)
			.build()

		updater.onMessageCreate(0, messageCreate).block()
	}


	private fun ready(selfId: Long) {
		val ready = Ready.builder()
			.user(user(selfId).build())
			.v(42)
			.sessionId(selfId.toString())
			.application(PartialApplicationInfoData.builder().id(selfId.toString()).build())
			.build()
		updater.onReady(ready)
	}

	private fun addReaction(guildId: Long, channelId: Long, messageId: Long, emoji: EmojiData, userId: Long) {
		val messageReactionAdd = MessageReactionAdd.builder()
			.guildId(guildId)
			.channelId(channelId)
			.messageId(messageId)
			.userId(userId)
			.member(member(userId).build())
			.emoji(emoji)
			.build()
		updater.onMessageReactionAdd(0, messageReactionAdd).block()
	}
}
