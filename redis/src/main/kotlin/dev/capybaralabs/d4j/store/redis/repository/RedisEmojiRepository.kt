package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.EmojiData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisEmojiRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), EmojiRepository {

	private val emojiKey = key("emoji")
	private val emojiOps = factory.createRedisHashOperations<String, Long, EmojiData>()

	override fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void> {
		return saveAll(guildId, listOf(emoji), shardId)
	}

	override fun saveAll(guildId: Long, emojis: List<EmojiData>, shardId: Int): Mono<Void> {
		val guildEmojis = emojis.filter { it.id().isPresent }
		if (guildEmojis.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			emojiOps.putAll(emojiKey, emojis.associateBy { it.id().get().asLong() }).then()
		}
	}

	override fun deleteByGuildId(emojiIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.defer {
			emojiOps.remove(emojiKey, *emojiIds.toTypedArray())
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.defer {
			emojiOps.size(emojiKey)
		}
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		// Ask the guild about it
		TODO("Not yet implemented")
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			emojiOps.values(emojiKey)
		}
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		// look up ids from guild repo
		TODO("Not yet implemented")
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.defer {
			emojiOps.get(emojiKey, emojiId)
		}
	}
}
