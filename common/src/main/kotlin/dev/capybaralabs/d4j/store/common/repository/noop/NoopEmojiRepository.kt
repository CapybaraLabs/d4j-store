package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.EmojiRepository
import discord4j.discordjson.json.EmojiData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopEmojiRepository : EmojiRepository {

	override fun save(guildId: Long, emoji: EmojiData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(emojisByGuild: Map<Long, List<EmojiData>>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteByIds(emojiIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countEmojis(): Mono<Long> {
		return Mono.empty()
	}

	override fun countEmojisInGuild(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun getEmojis(): Flux<EmojiData> {
		return Flux.empty()
	}

	override fun getEmojisInGuild(guildId: Long): Flux<EmojiData> {
		return Flux.empty()
	}

	override fun getEmojiById(guildId: Long, emojiId: Long): Mono<EmojiData> {
		return Mono.empty()
	}
}
