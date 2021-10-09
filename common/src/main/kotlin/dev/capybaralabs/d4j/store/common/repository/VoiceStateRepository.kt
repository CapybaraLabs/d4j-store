package dev.capybaralabs.d4j.store.common.repository

import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface VoiceStateRepository {
	fun save(voiceState: VoiceStateData, shardIndex: Int): Mono<Void>
	fun saveAll(voiceStates: List<VoiceStateData>, shardIndex: Int): Mono<Void>

	fun deleteById(guildId: Long, userId: Long): Mono<Int>
	fun deleteByGuildId(guildId: Long): Mono<Int>
	fun deleteByShardIndex(shardIndex: Int): Mono<Int>

	fun countVoiceStates(): Mono<Long>
	fun countVoiceStatesInGuild(guildId: Long): Mono<Long>
	fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long>

	fun getVoiceStates(): Flux<VoiceStateData>
	fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData>
	fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData>
	fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData>
}
