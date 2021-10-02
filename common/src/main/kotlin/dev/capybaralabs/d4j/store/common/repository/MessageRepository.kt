package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.MessageData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface MessageRepository {
	fun save(message: MessageData, shardIndex: Int): Mono<Void>
	fun delete(messageId: Long): Mono<Int>
	fun deleteByIds(messageIds: List<Long>): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>
	fun deleteByChannelId(channelId: Long): Mono<Int>
	fun deleteByChannelIds(channelIds: List<Long>): Mono<Int>
	fun countMessages(): Mono<Long>
	fun countMessagesInChannel(channelId: Long): Mono<Long>
	fun getMessages(): Flux<MessageData>
	fun getMessagesInChannel(channelId: Long): Flux<MessageData>
	fun getMessageById(messageId: Long): Mono<MessageData>
	fun getMessagesByIds(messageIds: List<Long>): Flux<MessageData>
}
