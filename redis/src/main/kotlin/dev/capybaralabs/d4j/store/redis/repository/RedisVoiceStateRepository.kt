package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisVoiceStateRepository : VoiceStateRepository {
	override fun save(voiceState: VoiceStateData, shardIndex: Int): Mono<Void> {
		TODO("Not yet implemented")
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardIndex: Int): Flux<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStates(): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStates(): Flux<VoiceStateData> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		TODO("Not yet implemented")
	}
}
