package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.StickerData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface StickerRepository {
	fun save(guildId: Long, sticker: StickerData, shardId: Int): Mono<Void>
	fun saveAll(stickers: List<StickerData>, shardId: Int): Mono<Void>

	fun deleteByIds(stickerIds: List<Long>, guildId: Long): Mono<Long>
	fun deleteByGuildId(guildId: Long): Mono<Long>
	fun deleteByShardId(shardId: Int): Mono<Long>

	fun countStickers(): Mono<Long>
	fun countStickersInGuild(guildId: Long): Mono<Long>

	fun getStickers(): Flux<StickerData>
	fun getStickersInGuild(guildId: Long): Flux<StickerData>
	fun getStickerById(guildId: Long, stickerId: Long): Mono<StickerData>
}
