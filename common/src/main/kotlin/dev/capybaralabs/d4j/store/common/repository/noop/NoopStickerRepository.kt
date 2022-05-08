package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.StickerRepository
import discord4j.discordjson.json.StickerData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopStickerRepository : StickerRepository {

	override fun saveAll(stickers: List<StickerData>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteByIds(stickerIds: List<Long>, guildId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.just(0)
	}

	override fun countStickers(): Mono<Long> {
		return Mono.just(0)
	}

	override fun countStickersInGuild(guildId: Long): Mono<Long> {
		return Mono.just(0)
	}

	override fun getStickers(): Flux<StickerData> {
		return Flux.empty()
	}

	override fun getStickersInGuild(guildId: Long): Flux<StickerData> {
		return Flux.empty()
	}

	override fun getStickerById(guildId: Long, stickerId: Long): Mono<StickerData> {
		return Mono.empty()
	}
}
