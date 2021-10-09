package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.EmojiData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisEmojiRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), EmojiRepository {

	private val hash = hash("emoji")
	private val hashOps = factory.createRedisHashOperations<String, Long, EmojiData>()

	override fun save(guildId: Long, emoji: EmojiData, shardIndex: Int): Mono<Void> {
		return saveAll(guildId, listOf(emoji), shardIndex)
	}

	override fun saveAll(guildId: Long, emojis: List<EmojiData>, shardIndex: Int): Mono<Void> {
		val guildEmojis = emojis.filter { it.id().isPresent }
		if (guildEmojis.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			hashOps.putAll(hash, emojis.associateBy { it.id().get().asLong() }).then()
		}
	}

	override fun deleteByIds(emojiIds: List<Long>): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, *emojiIds.toTypedArray())
				.map { toIntExact(it) }
		}
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		// look up ids from shard repo
		TODO("Not yet implemented")
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
		}
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		// Ask the guild about it
		TODO("Not yet implemented")
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.defer {
			hashOps.values(hash)
		}
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		// look up ids from guild repo
		TODO("Not yet implemented")
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.defer {
			hashOps.get(hash, emojiId)
		}
	}
}
