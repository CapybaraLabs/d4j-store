package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.ImmutableReactionData
import discord4j.discordjson.json.PartialApplicationInfoData
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.json.gateway.MessageReactionAdd
import discord4j.discordjson.json.gateway.MessageReactionRemove
import discord4j.discordjson.json.gateway.MessageReactionRemoveAll
import discord4j.discordjson.json.gateway.MessageReactionRemoveEmoji
import discord4j.discordjson.json.gateway.Ready
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Isolated

@Isolated("use of onReady global state")
@TestMethodOrder(value = OrderAnnotation::class)
internal class ReactionTest {

	@Test
	@Order(1)
	fun givenNoReady_onMessageReactionAdd_throw() {
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		createMessage(channelId, messageId)

		assertThatThrownBy { addReaction(channelId, messageId, emoji(emojiId).build(), userId) }
			.isInstanceOf(IllegalStateException::class.java)
	}

	// TODO find better way to order tests using onReady
	@Test
	fun onReady_createSelfUser() {
		val selfId = generateUniqueSnowflakeId()

		val ready = Ready.builder()
			.user(user(selfId).username("Benjamin Sisko").build())
			.v(42)
			.sessionId("Season 1")
			.application(PartialApplicationInfoData.builder().id(selfId.toString()).build())
			.build()
		updater.onReady(ready).block()

		assertThat(accessor.getUserById(selfId).block())
			.matches { it.id().asLong() == selfId && it.username() == "Benjamin Sisko" }
	}


	// onMessageReactionAdd
	@Test
	fun givenMessageWithoutReactions_onMessageReactionAdd_createReaction() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)

		addReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithoutReactions_onMessageUnicodeReactionAdd_createReaction() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)

		addReaction(channelId, messageId, unicodeEmoji("👌").build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().name().get() == "👌" && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithReaction_onMessageReactionAdd_incrementCount() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiId)

		addReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 2 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithoutReactions_onMultipleMessageReactionAdd_createMultipleReactions() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiIdA)

		addReaction(channelId, messageId, emoji(emojiIdA).build(), userId)
		addReaction(channelId, messageId, emoji(emojiIdB).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(2)
					.anyMatch { it.emoji().id().get().asLong() == emojiIdA && it.count() == 2 && !it.me() }
					.anyMatch { it.emoji().id().get().asLong() == emojiIdB && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithReactions_onMessageReactionAddBySelf_updateReactionIsMe() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, authorId, emojiId)

		addReaction(channelId, messageId, emoji(emojiId).build(), selfId)

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
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)

		addReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}

	// onMessageReactionRemove
	@Test
	fun givenMessageWithReaction_onMessageReactionRemove_decreaseCount() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiId)
		addReaction(channelId, messageId, emoji(emojiId).build(), userId)
		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 2 && !it.me() }
			}

		removeReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithReactions_onMessageReactionRemove_removeReaction() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiIdA)
		addReaction(channelId, messageId, emoji(emojiIdB).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(2)
					.anyMatch { it.emoji().id().get().asLong() == emojiIdA && it.count() == 1 && !it.me() }
					.anyMatch { it.emoji().id().get().asLong() == emojiIdB && it.count() == 1 && !it.me() }
			}

		removeReaction(channelId, messageId, emoji(emojiIdA).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiIdB && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithReactions_onMessageUnicodeReactionRemove_decreaseCount() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)
		addReaction(channelId, messageId, unicodeEmoji("👌").build(), userIdA)
		addReaction(channelId, messageId, unicodeEmoji("👌").build(), userIdB)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().name().get() == "👌" && it.count() == 2 && !it.me() }
			}

		removeReaction(channelId, messageId, unicodeEmoji("👌").build(), userIdA)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().name().get() == "👌" && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithSingleReaction_onMessageReactionRemove_removeReactions() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 1 && !it.me() }
			}

		removeReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageWithReactionByMe_onMessageReactionRemoveBySelf_updateReactionIsNotMe() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiId)
		addReaction(channelId, messageId, emoji(emojiId).build(), selfId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 2 && it.me() }
			}

		removeReaction(channelId, messageId, emoji(emojiId).build(), selfId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.anyMatch { it.emoji().id().get().asLong() == emojiId && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithoutReactions_onMessageReactionRemove_noChanges() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)
		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }

		removeReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageNotExists_onMessageReactionRemove_doNothing() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)

		removeReaction(channelId, messageId, emoji(emojiId).build(), userId)

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}

	// onMessageReactionRemoveAll
	@Test
	fun givenMessageWithReactions_onMessageReactionRemoveAll_removeAllReactions() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiIdA)
		addReaction(channelId, messageId, emoji(emojiIdB).build(), userId)

		removeAllReactions(channelId, messageId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageWithoutReactions_onMessageReactionRemoveAll_noChanges() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)
		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }

		removeAllReactions(channelId, messageId)

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageNotExists_onMessageReactionRemoveAll_doNothing() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		ready(selfId)

		removeAllReactions(channelId, messageId)

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}

	// onMessageReactionRemoveEmoji
	@Test
	fun givenMessageWithReactions_onMessageReactionRemoveEmoji_removeReactionEmoji() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiIdA)
		addReaction(channelId, messageId, emoji(emojiIdA).build(), userId)
		addReaction(channelId, messageId, emoji(emojiIdB).build(), userId)

		removeReactionEmoji(channelId, messageId, emoji(emojiIdA).build())

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.noneMatch { it.emoji().id().get().asLong() == emojiIdA }
					.anyMatch { it.emoji().id().get().asLong() == emojiIdB && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithUnicodeReaction_onMessageReactionRemoveUnicodeEmoji_removeReactionEmoji() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)
		addReaction(channelId, messageId, unicodeEmoji("👌").build(), userId)
		addReaction(channelId, messageId, unicodeEmoji("👍").build(), userId)

		removeReactionEmoji(channelId, messageId, unicodeEmoji("👌").build())

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.noneMatch { it.emoji().name().get() == "👌" }
					.anyMatch { it.emoji().name().get() == "👍" && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageWithReaction_onMessageReactionRemoveEmoji_removeReactions() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val emojiId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessageWithReaction(channelId, messageId, generateUniqueSnowflakeId(), emojiId)

		removeReactionEmoji(channelId, messageId, emoji(emojiId).build())

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageWithoutReactions_onMessageReactionRemoveEmoji_noChanges() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)

		removeReactionEmoji(channelId, messageId, unicodeEmoji("👌").build())

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.matches { it.reactions().isAbsent }
	}

	@Test
	fun givenMessageWithoutReaction_onMessageReactionRemoveEmoji_noChanges() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		val userId = generateUniqueSnowflakeId()
		ready(selfId)
		createMessage(channelId, messageId)
		addReaction(channelId, messageId, unicodeEmoji("👌").build(), userId)

		removeReactionEmoji(channelId, messageId, unicodeEmoji("👍").build())

		assertThat(accessor.getMessageById(channelId, messageId).block())
			.satisfies { message ->
				assertThat(message.reactions().get())
					.hasSize(1)
					.noneMatch { it.emoji().name().get() == "👍" }
					.anyMatch { it.emoji().name().get() == "👌" && it.count() == 1 && !it.me() }
			}
	}

	@Test
	fun givenMessageNotExists_onMessageReactionRemoveEmoji_doNothing() {
		val selfId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val messageId = generateUniqueSnowflakeId()
		ready(selfId)

		removeReactionEmoji(channelId, messageId, unicodeEmoji("👍").build())

		assertThat(accessor.getMessageById(channelId, messageId).block()).isNull()
	}


	private fun createMessage(channelId: Long, messageId: Long) {
		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, generateUniqueSnowflakeId()).build())
			.build()

		updater.onMessageCreate(0, messageCreate).block()
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
		updater.onReady(ready).block()
	}

	private fun addReaction(channelId: Long, messageId: Long, emoji: EmojiData, userId: Long) {
		val messageReactionAdd = MessageReactionAdd.builder()
			.channelId(channelId)
			.messageId(messageId)
			.userId(userId)
			.member(member(userId).build())
			.emoji(emoji)
			.build()
		updater.onMessageReactionAdd(0, messageReactionAdd).block()
	}

	private fun removeReaction(channelId: Long, messageId: Long, emoji: EmojiData, userId: Long) {
		val reactionRemove = MessageReactionRemove.builder()
			.channelId(channelId)
			.messageId(messageId)
			.userId(userId)
			.emoji(emoji)
			.build()
		updater.onMessageReactionRemove(0, reactionRemove).block()
	}

	private fun removeAllReactions(channelId: Long, messageId: Long) {
		val reactionRemoveAll = MessageReactionRemoveAll.builder()
			.channelId(channelId)
			.messageId(messageId)
			.build()
		updater.onMessageReactionRemoveAll(0, reactionRemoveAll).block()
	}

	private fun removeReactionEmoji(channelId: Long, messageId: Long, emoji: EmojiData) {
		val reactionRemoveEmoji = MessageReactionRemoveEmoji.builder()
			.channelId(channelId)
			.messageId(messageId)
			.emoji(emoji)
			.build()
		updater.onMessageReactionRemoveEmoji(0, reactionRemoveEmoji).block()
	}
}
