package dev.capybaralabs.d4j.store.tck

import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.MessageCreate
import discord4j.discordjson.json.gateway.MessageDelete
import discord4j.discordjson.json.gateway.MessageDeleteBulk
import discord4j.discordjson.json.gateway.MessageUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MessageTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

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
}
