package dev.capybaralabs.d4j.store.common.repository.noop

import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class NoopVoiceStateRepository : VoiceStateRepository {
	override fun save(voiceState: VoiceStateData, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardId: Int): Mono<Void> {
		return Mono.empty()
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.empty()
	}

	override fun countVoiceStates(): Mono<Long> {
		return Mono.empty()
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return Mono.empty()
	}

	override fun getVoiceStates(): Flux<VoiceStateData> {
		return Flux.empty()
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return Flux.empty()
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return Flux.empty()
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return Mono.empty()
	}
}
