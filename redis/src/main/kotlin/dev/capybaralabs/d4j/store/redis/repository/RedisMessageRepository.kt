package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MessageData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMessageRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MessageRepository {

	private val messageKey = key("message")
	private val messageOps = factory.createRedisHashOperations<String, Long, MessageData>()

	override fun save(message: MessageData, shardId: Int): Mono<Void> {
		return Mono.defer {
			messageOps.put(messageKey, message.id().asLong(), message).then()
		}
	}

	override fun delete(messageId: Long): Mono<Long> {
		return Mono.defer {
			messageOps.remove(messageKey, messageId)
		}
	}

	override fun deleteByIds(messageIds: List<Long>): Mono<Long> {
		return Mono.defer {
			messageOps.remove(messageKey, *messageIds.toTypedArray())
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelId(channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countMessages(): Mono<Long> {
		return Mono.defer {
			messageOps.size(messageKey)
		}
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getMessages(): Flux<MessageData> {
		return Flux.defer {
			messageOps.values(messageKey)
		}
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		TODO("Not yet implemented")
	}

	override fun getMessageById(messageId: Long): Mono<MessageData> {
		return Mono.defer {
			messageOps.get(messageKey, messageId)
		}
	}

	override fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData> {
		return Flux.defer {
			messageOps.multiGet(messageKey, messageIds)
				.flatMapIterable { it }
		}
	}
}
