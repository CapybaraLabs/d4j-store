package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisVoiceStateRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), VoiceStateRepository {

	private val voiceStateKey = key("voicestate")
	private val voiceStateOps = factory.createRedisHashOperations<String, String, VoiceStateData>()

	private fun voiceStateId(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(voiceState: VoiceStateData, shardId: Int): Mono<Void> {
		return saveAll(listOf(voiceState), shardId).then()
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardId: Int): Mono<Void> {
		if (voiceStates.isEmpty()) {
			return Mono.empty()
		}

		val voiceStatesInChannels = voiceStates.filter { it.channelId().isPresent && it.guildId().isPresent() }
		if (voiceStatesInChannels.isEmpty()) {
			return Mono.empty()
		}

		return Mono.defer {
			voiceStateOps.putAll(voiceStateKey, voiceStatesInChannels
				.associateBy {
					voiceStateId(it.guildId().get().asLong(), it.userId().asLong())
				})
				.then()
		}

	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			voiceStateOps.remove(voiceStateKey, voiceStateId(guildId, userId))
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStates(): Mono<Long> {
		return Mono.defer {
			voiceStateOps.size(voiceStateKey)
		}
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStates(): Flux<VoiceStateData> {
		return Flux.defer {
			voiceStateOps.values(voiceStateKey)
		}
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		TODO("Not yet implemented")
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return Mono.defer {
			voiceStateOps.get(voiceStateKey, voiceStateId(guildId, userId))
		}
	}
}
