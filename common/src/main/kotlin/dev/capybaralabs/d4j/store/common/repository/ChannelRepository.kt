package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.ChannelData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ChannelRepository {
	fun save(channel: ChannelData, shardIndex: Int): Mono<Void>
	fun saveAll(channels: List<ChannelData>, shardIndex: Int): Mono<Void>

	fun delete(channelId: Long): Mono<Int>
	fun deleteByIds(channelIds: List<Long>): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>

	fun countChannels(): Mono<Long>
	fun countChannelsInGuild(guildId: Long): Mono<Long>

	fun getChannelById(channelId: Long): Mono<ChannelData>
	fun getChannels(): Flux<ChannelData>
	fun getChannelsInGuild(guildId: Long): Flux<ChannelData>
}
