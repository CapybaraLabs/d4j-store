package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.repository.MessageRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.MessageData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisMessageRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), MessageRepository {

	private val messageKey = key("message")
	private val messageOps = factory.createRedisHashOperations<String, Long, MessageData>()

	private val shardIndex = twoWayIndex("$messageKey:shard-index", factory)
	private val channelIndex = oneWayIndex("$messageKey:channel-index", factory)
	private val channelShardIndex = twoWayIndex("$messageKey:channel-shard-index", factory)

	override fun save(message: MessageData, shardId: Int): Mono<Void> {
		return Mono.defer {
			val messageId = message.id().asLong()
			val channelId = message.channelId().asLong()

			val addToShardIndex = shardIndex.addElements(shardId, listOf(messageId))
			val addToChannelIndex = channelIndex.addElements(channelId, messageId)
			val addToChannelShardIndex = channelShardIndex.addElements(shardId, listOf(channelId))

			val save = messageOps.put(messageKey, messageId, message)

			Mono.`when`(addToShardIndex, addToChannelIndex, addToChannelShardIndex, save)
		}
	}

	override fun delete(messageId: Long, channelId: Long): Mono<Long> {
		return deleteByIds(listOf(messageId), channelId)
	}

	override fun deleteByIds(messageIds: List<Long>, channelId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromShardIndex = shardIndex.removeElements(*messageIds.toTypedArray())
			val removeFromChannelIndex = channelIndex.removeElements(channelId, *messageIds.toTypedArray())

			val remove = messageOps.remove(messageKey, *messageIds.toTypedArray())

			Mono.`when`(removeFromShardIndex, removeFromChannelIndex)
				.then(remove)
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			val getIds: Mono<Set<Long>> = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
			val getChannelIds: Mono<Set<Long>> = channelShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { channelShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }

			val getAllIds = getChannelIds.flatMap { channelIds ->
				// Technically we don't need to fetch the ids from the groups here, we can rely on the shard index only.
				channelIndex.getElementsInGroups(channelIds).collectSet()
					.flatMap { channelIndex.deleteGroups(channelIds).then(Mono.just(it)) }
					.flatMap { idsInChannels -> getIds.map { ids -> idsInChannels + ids } }
			}

			getAllIds.flatMap { messageOps.remove(messageKey, *it.toTypedArray()) }
		}
	}

	override fun deleteByChannelId(channelId: Long): Mono<Long> {
		return deleteByChannelIds(listOf(channelId))
	}

	override fun deleteByChannelIds(channelIds: List<Long>): Mono<Long> {
		return Mono.defer {
			channelIndex.getElementsInGroups(channelIds).collectList()
				.flatMap { ids ->
					val removeFromShardIndex = shardIndex.removeElements(*ids.toTypedArray())
					val deleteChannelIndexEntries = channelIndex.deleteGroups(channelIds)
					val removeChannelsFromShardIndex = channelShardIndex.removeElements(*channelIds.toTypedArray())

					val remove = messageOps.remove(messageKey, *ids.toTypedArray())

					Mono.`when`(removeFromShardIndex, deleteChannelIndexEntries, removeChannelsFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun countMessages(): Mono<Long> {
		return Mono.defer {
			messageOps.size(messageKey)
		}
	}

	override fun countMessagesInChannel(channelId: Long): Mono<Long> {
		return Mono.defer {
			channelIndex.countElementsInGroup(channelId)
		}
	}

	override fun getMessages(): Flux<MessageData> {
		return Flux.defer {
			messageOps.values(messageKey)
		}
	}

	override fun getMessagesInChannel(channelId: Long): Flux<MessageData> {
		return Flux.defer {
			channelIndex.getElementsInGroup(channelId).collectList()
				.flatMap { messageOps.multiGet(messageKey, it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
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
