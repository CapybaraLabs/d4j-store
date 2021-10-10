package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MessageData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMessageRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MessageRepository {

	private val hash = key("message")
	private val hashOps = factory.createRedisHashOperations<String, Long, MessageData>()

	override fun save(message: MessageData, shardId: Int): Mono<Void> {
		return Mono.defer {
			hashOps.put(hash, message.id().asLong(), message).then()
		}
	}

	override fun delete(messageId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, messageId)
				.map { toIntExact(it) }
		}
	}

	override fun deleteByIds(messageIds: List<Long>): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, *messageIds.toTypedArray())
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelId(channelId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countMessages(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getMessages(): Flux<MessageData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		TODO("Not yet implemented")
	}

	override fun getMessageById(messageId: Long): Mono<MessageData> {
		return Mono.defer {
			hashOps.get(hash, messageId)
		}
	}

	override fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData> {
		return Flux.defer {
			hashOps.multiGet(hash, messageIds)
				.flatMapIterable { it }
		}
	}
}
