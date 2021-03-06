package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.VoiceStateRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.VoiceStateData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class RedisVoiceStateRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), VoiceStateRepository {

	private val voiceStateKey = key("voicestate")
	private val voiceStateOps = RedisHashOps(voiceStateKey, factory, String::class.java, VoiceStateData::class.java)

	// a user can be in multiple channels (e.g. bots), so our indices have to handle that
	// kinda dumb to have the guildId as part of the index values here BUT we need it because it is not unique
	private val guildIndex = oneWayIndex("$voiceStateKey:guild-index", factory, String::class.java)
	private val gShardIndex = twoWayIndex("$voiceStateKey:guild-shard-index", factory)

	private fun voiceStateId(guildId: Long, userId: Long): String {
		return "$guildId:$userId"
	}

	override fun save(voiceState: VoiceStateData, shardId: Int): Mono<Void> {
		return saveAll(listOf(voiceState), shardId)
	}

	override fun saveAll(voiceStates: List<VoiceStateData>, shardId: Int): Mono<Void> {
		val voiceStatesInChannels = voiceStates.filter { it.channelId().isPresent && it.guildId().isPresent() }
		if (voiceStatesInChannels.isEmpty()) {
			return Mono.empty()
		}

		val voiceStatesByGuild = voiceStatesInChannels.groupBy { it.guildId().get().asLong() }
		val voiceStatesById = voiceStatesInChannels.associateBy { voiceStateId(it.guildId().get().asLong(), it.userId().asLong()) }

		return Mono.defer {
			val addToGuildIndex = Flux.fromIterable(
				voiceStatesByGuild.map { entry ->
					val guildId = entry.key
					val userIds = entry.value.map { it.userId().asLong() }
					guildIndex.addElements(guildId, *userIds.map { voiceStateId(guildId, it) }.toTypedArray())
				}
			).flatMap { it }
			val addGuildToShardIndex = gShardIndex.addElements(shardId, voiceStatesByGuild.keys)

			val save = voiceStateOps.putAll(voiceStatesById)

			Mono.`when`(addToGuildIndex, addGuildToShardIndex, save)
		}
	}

	override fun deleteById(guildId: Long, userId: Long): Mono<Long> {
		return Mono.defer {
			val removeFromGuildIndex = guildIndex.removeElements(guildId, voiceStateId(guildId, userId))
			val remove = voiceStateOps.remove(voiceStateId(guildId, userId))

			Mono.`when`(removeFromGuildIndex)
				.then(remove)
		}
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { voiceStateIds ->
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					val remove = voiceStateOps.remove(*voiceStateIds.toTypedArray())

					Mono.`when`(deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(remove)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		return Mono.defer {
			gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { guildIds ->
					val fetchVoiceStateIds = guildIndex.getElementsInGroups(guildIds).collectList()

					val removeGuildsFromShardIndex = gShardIndex.deleteByGroupId(shardId)
					val deleteGuildsFromGuildIndex = guildIndex.deleteGroups(guildIds)

					fetchVoiceStateIds.flatMap { voiceStateIds ->
						Mono.`when`(removeGuildsFromShardIndex, deleteGuildsFromGuildIndex).then(Mono.just(voiceStateIds))
					}
				}
				.flatMap { voiceStateIds -> voiceStateOps.remove(*voiceStateIds.toTypedArray()) }
		}
	}

	override fun countVoiceStates(): Mono<Long> {
		return Mono.defer {
			voiceStateOps.size()
		}
	}

	override fun countVoiceStatesInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}

	override fun countVoiceStatesInChannel(guildId: Long, channelId: Long): Mono<Long> {
		return Mono.defer {
			// somewhat inefficient, but we avoid maintaining a complicated channel index
			getVoiceStatesInChannel(guildId, channelId)
				.collectList().map { it.size.toLong() }
		}
	}

	override fun getVoiceStates(): Flux<VoiceStateData> {
		return Flux.defer {
			voiceStateOps.values()
		}
	}

	override fun getVoiceStatesInChannel(guildId: Long, channelId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			// somewhat inefficient, but we avoid maintaining a complicated channel index
			getVoiceStatesInGuild(guildId)
				.filter { it.channelId().isPresent }
				.filter { it.channelId().get().asLong() == channelId }
		}
	}

	override fun getVoiceStatesInGuild(guildId: Long): Flux<VoiceStateData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { voiceStateOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}

	override fun getVoiceStateById(guildId: Long, userId: Long): Mono<VoiceStateData> {
		return Mono.defer {
			voiceStateOps.get(voiceStateId(guildId, userId))
		}
	}
}
