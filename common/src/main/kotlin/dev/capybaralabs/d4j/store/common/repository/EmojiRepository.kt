package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.EmojiData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface EmojiRepository {
	fun save(guildId: Long, emoji: EmojiData, shardIndex: Int): Mono<Void>
	fun saveAll(guildId: Long, emojis: List<EmojiData>, shardIndex: Int): Flux<Int>
	fun deleteByIds(emojiIds: List<Long>): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>
	fun countEmojis(): Mono<Long>
	fun countEmojisInGuild(guildId: Long): Mono<Long>
	fun getEmojis(): Flux<EmojiData>
	fun getEmojisInGuild(guildId: Long): Flux<EmojiData>
	fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData>
}
