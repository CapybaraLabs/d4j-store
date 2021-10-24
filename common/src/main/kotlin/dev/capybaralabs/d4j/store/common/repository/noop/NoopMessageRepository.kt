package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import discord4j.discordjson.json.MessageData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopMessageRepository : MessageRepository {
	override fun save(message: MessageData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun delete(messageId: Long, channelId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByIds(messageIds: List<Long>, channelId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByChannelId(channelId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Long> {
		return Mono.just(0)
	}

	override fun countMessages(): Mono<Long> {
		return Mono.just(0)
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun getMessages(): Flux<MessageData> {
		return Flux.empty()
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		return Flux.empty()
	}

	override fun getMessageById(messageId: Long): Mono<MessageData> {
		return Mono.empty()
	}

	override fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData> {
		return Flux.empty()
	}
}
