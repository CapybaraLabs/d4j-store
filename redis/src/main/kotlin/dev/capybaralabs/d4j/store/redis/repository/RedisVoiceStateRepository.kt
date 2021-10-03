package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.VoiceStateData
import java.lang.StrictMath.toIntExact
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class RedisVoiceStateRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), VoiceStateRepository {

	private val hash = hash("voicestate")
	private val hashOps = factory.createRedisHashOperations<String, String, VoiceStateData>()

	private fun voiceStateKey(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(voiceState: VoiceStateData, shardIndex: Int): Mono<Void> {
		return saveAll(listOf(voiceState), shardIndex).then()
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardIndex: Int): Flux<Int> {
		if (voiceStates.isEmpty()) {
			return Flux.empty()
		}

		val voiceStatesInChannels = voiceStates.filter { it.channelId().isPresent && it.guildId().isPresent() }
		if (voiceStatesInChannels.isEmpty()) {
			return Flux.empty()
		}

		return Flux.defer {
			hashOps.putAll(hash, voiceStatesInChannels
				.associateBy {
					voiceStateKey(it.guildId().get().asLong(), it.userId().asLong())
				})
				.map { if (it) 1 else 0 } // TODO rethink the signature of the method, it doesnt really make sense here
				.toFlux()
		}

	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Int> {
		return Mono.defer {
			hashOps.remove(hash, voiceStateKey(guildId, userId))
				.map { toIntExact(it) }
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun deleteByShardIndex(shardIndex: Int): Mono<Int> {
		TODO("Not yet implemented")
	}

	override fun countVoiceStates(): Mono<Long> {
		return Mono.defer {
			hashOps.size(hash)
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
			hashOps.values(hash)
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
			hashOps.get(hash, voiceStateKey(guildId, userId))
		}
	}
}
