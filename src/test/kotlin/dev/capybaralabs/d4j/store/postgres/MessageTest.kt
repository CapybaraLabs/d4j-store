package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.MessageCreate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MessageTest {

	@Test
	fun onMessageCreate() {
		val messageId = generateUniqueSnowflakeId()
		val channelId = generateUniqueSnowflakeId()
		val authorId = generateUniqueSnowflakeId()

		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, authorId).build())
			.build()

		updater.onChannelCreate(
			0, ChannelCreate.builder()
				.channel(channel(channelId).build())
				.build()
		).block()

		updater.onMessageCreate(0, messageCreate).block()

		val message = accessor.getMessageById(channelId, messageId).block()!!
		assertThat(message.id().asLong()).isEqualTo(messageId)
		assertThat(message.channelId().asLong()).isEqualTo(channelId)
		assertThat(message.author().id().asLong()).isEqualTo(authorId)
		assertThat(message.content()).isEqualTo("🖖")

		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isEqualTo(1)

		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.lastMessageId().get().get().asLong()).isEqualTo(messageId)
	}
}
