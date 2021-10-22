package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import discord4j.discordjson.json.ChannelData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopChannelRepository : ChannelRepository {

	override fun save(channel: ChannelData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(channels: List<ChannelData>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun delete(channelId: Long, guildId: Long?): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countChannels(): Mono<Long> {
		return Mono.empty()
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.empty()
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.empty()
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return Flux.empty()
	}
}
