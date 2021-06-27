package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.gateway.VoiceStateUpdateDispatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VoiceStateTest {

	@Test
	fun countVoiceStates() {
		assertThat(accessor.countVoiceStates().blockOptional()).isPresent
	}


	@Test
	fun whenJoiningChannel_onVoiceStateUpdate_createVoiceState() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val userIdC = generateUniqueSnowflakeId()

		joinVoiceChannel(guildId, channelIdA, userIdA)
		joinVoiceChannel(guildId, channelIdB, userIdB)
		joinVoiceChannel(guildId, channelIdB, userIdC)


		assertThat(accessor.getVoiceStateById(guildId, userIdA).block())
			.matches(isVoiceState(guildId, channelIdA, userIdA))
		assertThat(accessor.getVoiceStateById(guildId, userIdB).block())
			.matches(isVoiceState(guildId, channelIdB, userIdB))
		assertThat(accessor.getVoiceStateById(guildId, userIdC).block())
			.matches(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.voiceStates.collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!)
			.isEqualTo(1)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!)
			.isEqualTo(2)

		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!)
			.isEqualTo(3)
	}

	@Test
	fun whenLeavingChannel_onVoiceStateUpdate_deleteVoiceState() {
		val guildId = generateUniqueSnowflakeId()
		val channelIdA = generateUniqueSnowflakeId()
		val channelIdB = generateUniqueSnowflakeId()
		val userIdA = generateUniqueSnowflakeId()
		val userIdB = generateUniqueSnowflakeId()
		val userIdC = generateUniqueSnowflakeId()

		joinVoiceChannel(guildId, channelIdA, userIdA)
		joinVoiceChannel(guildId, channelIdB, userIdB)
		joinVoiceChannel(guildId, channelIdB, userIdC)

		leaveVoiceChannel(guildId, userIdA)
		leaveVoiceChannel(guildId, userIdC)

		assertThat(accessor.getVoiceStateById(guildId, userIdA).block()).isNull()
		assertThat(accessor.getVoiceStateById(guildId, userIdB).block())
			.matches(isVoiceState(guildId, channelIdB, userIdB))
		assertThat(accessor.getVoiceStateById(guildId, userIdC).block()).isNull()

		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdA).collectList().block()).isEmpty()
		assertThat(accessor.getVoiceStatesInChannel(guildId, channelIdB).collectList().block())
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.getVoiceStatesInGuild(guildId).collectList().block())
			.noneMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.voiceStates.collectList().block())
			.noneMatch(isVoiceState(guildId, channelIdA, userIdA))
			.anyMatch(isVoiceState(guildId, channelIdB, userIdB))
			.noneMatch(isVoiceState(guildId, channelIdB, userIdC))

		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdA).block()!!)
			.isEqualTo(0)
		assertThat(accessor.countVoiceStatesInChannel(guildId, channelIdB).block()!!)
			.isEqualTo(1)

		assertThat(accessor.countVoiceStatesInGuild(guildId).block()!!)
			.isEqualTo(1)
	}

	private fun joinVoiceChannel(guildId: Long, channelId: Long, userId: Long) {
		val voiceStateUpdate = VoiceStateUpdateDispatch.builder()
			.voiceState(voiceStateInChannel(guildId, channelId, userId).build())
			.build()
		updater.onVoiceStateUpdateDispatch(0, voiceStateUpdate).block()
	}

	private fun leaveVoiceChannel(guildId: Long, userId: Long) {
		val voiceStateUpdate = VoiceStateUpdateDispatch.builder()
			.voiceState(voiceStateNoChannel(guildId, userId).build())
			.build()
		updater.onVoiceStateUpdateDispatch(0, voiceStateUpdate).block()
	}
}
