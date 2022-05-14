package dev.capybaralabs.d4j.store.common.repository.flag

import dev.capybaralabs.d4j.store.common.repository.PresenceRepository
import discord4j.discordjson.json.PartialUserData
import discord4j.discordjson.json.PresenceData
import reactor.core.publisher.Mono

class MinimalUserPresenceRepository(private val delegate: PresenceRepository) : PresenceRepository by delegate {

	override fun save(guildId: Long, presence: PresenceData, shardId: Int): Mono<Void> {
		return delegate.save(guildId, minimalData(presence), shardId)
	}

	override fun saveAll(presencesByGuild: Map<Long, List<PresenceData>>, shardId: Int): Mono<Void> {
		val minimalData = presencesByGuild.mapValues { it.value.map { p -> minimalData(p) } }
		return saveAll(minimalData, shardId)
	}

	private fun minimalData(presence: PresenceData): PresenceData {
		val minimalUserData = PartialUserData.builder()
			.id(presence.user().id())
			.build();

		return PresenceData.builder()
			.from(presence)
			.user(minimalUserData)
			.build()
	}
}
