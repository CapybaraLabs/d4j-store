package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import discord4j.discordjson.json.EmojiData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisEmojiRepository : EmojiRepository {
	override fun save(guildId: Long, emoji: EmojiData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(guildId: Long, emojis: List<EmojiData>, shardIndex: Int): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByIds(emojiIds: List<Long>): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countEmojis(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getEmojis(): Flux<EmojiData> {
		TODO("Not yet implemented")
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		TODO("Not yet implemented")
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		TODO("Not yet implemented")
	}
}
