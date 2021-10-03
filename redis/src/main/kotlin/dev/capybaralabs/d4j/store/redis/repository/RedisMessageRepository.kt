package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import discord4j.discordjson.json.MessageData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMessageRepository : MessageRepository {
	override fun save(message: MessageData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun delete(messageId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByIds(messageIds: List<Long>): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelId(channelId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countMessages(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getMessages(): Flux<MessageData> {
		TODO("Not yet implemented")
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		TODO("Not yet implemented")
	}

	override fun getMessageById(messageId: Long): Mono<MessageData> {
		TODO("Not yet implemented")
	}

	override fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData> {
		TODO("Not yet implemented")
	}
}
