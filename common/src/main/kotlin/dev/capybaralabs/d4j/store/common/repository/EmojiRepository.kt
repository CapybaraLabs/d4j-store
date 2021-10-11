package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.EmojiData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface EmojiRepository {
	fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void>
	fun saveAll(guildId: Long, emojis: List<EmojiData>, shardId: Int): Mono<Void>

	fun deleteByGuildId(emojiIds: List<Long>, guildId: Long): Mono<Long>
	fun deleteByShardId(shardId: Int): Mono<Long>

	fun countEmojis(): Mono<Long>
	fun countEmojisInGuild(guildId: Long): Mono<Long>

	fun getEmojis(): Flux<EmojiData>
	fun getEmojisInGuild(guildId: Long): Flux<EmojiData>
	fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData>
}
