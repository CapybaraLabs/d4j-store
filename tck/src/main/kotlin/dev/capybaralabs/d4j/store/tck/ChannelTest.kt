package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import discord4j.discordjson.Id
import discord4j.discordjson.json.gateway.ChannelCreate
import discord4j.discordjson.json.gateway.ChannelDelete
import discord4j.discordjson.json.gateway.ChannelUpdate
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.MessageCreate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ChannelTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun countChannels() {
		assertThat(accessor.countChannels().blockOptional()).isPresent
	}


	@Test
	fun onChannelCreate_createChannel() {
		val channelId = generateUniqueSnowflakeId()

		val channelCreate = ChannelCreate.builder()
			.channel(channel(channelId).name("Emergency Medical Holographic Channel").build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.id().asLong()).isEqualTo(channelId)
		assertThat(channel.name().isAbsent).isFalse
		assertThat(channel.name().get()).isEqualTo("Emergency Medical Holographic Channel")
		assertThat(channel.guildId().isAbsent).isTrue

		assertThat(accessor.channels.collectList().block())
			.anyMatch { it.id().asLong() == channelId }
		assertThat(accessor.countChannels().block()!!).isGreaterThan(0)
	}

	@Test
	fun givenChannelInGuild_onChannelCreate_addChannelToGuild() {
		val channelId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		val channelCreate = ChannelCreate.builder().channel(channel(channelId).guildId(guildId).build())
		updater.onChannelCreate(0, channelCreate.build()).block()


		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.id().asLong()).isEqualTo(channelId)
		assertThat(channel.guildId().isAbsent).isFalse
		assertThat(channel.guildId().get().asLong()).isEqualTo(guildId)

		assertThat(accessor.getGuildById(guildId).block()!!.channels())
			.contains(Id.of(channelId))
		assertThat(accessor.countChannelsInGuild(guildId).block()!!)
			.isEqualTo(1)
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block())
			.anyMatch { it.id().asLong() == channelId }
	}

	@Test
	fun onChannelDelete_deleteChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelCreate = ChannelCreate.builder().channel(channel(channelId).build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.id().asLong()).isEqualTo(channelId)

		val channelDelete = ChannelDelete.builder().channel(channel(channelId).build())
		updater.onChannelDelete(0, channelDelete.build()).block()

		assertThat(accessor.getChannelById(channelId).block()).isNull()
	}

	@Test
	fun givenChannelInGuild_onChannelDelete_removeChannelFromGuild() {
		val channelId = generateUniqueSnowflakeId()
		val guildId = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(guild(guildId).build())
			.build()
		updater.onGuildCreate(0, guildCreate).block()

		val channelCreate = ChannelCreate.builder().channel(channel(channelId).guildId(guildId).build())
		updater.onChannelCreate(0, channelCreate.build()).block()


		assertThat(accessor.getGuildById(guildId).block()!!.channels())
			.contains(Id.of(channelId))
		assertThat(accessor.countChannelsInGuild(guildId).block()!!)
			.isEqualTo(1)
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block())
			.anyMatch { it.id().asLong() == channelId }


		val channelDelete = ChannelDelete.builder().channel(channel(channelId).guildId(guildId).build())
		updater.onChannelDelete(0, channelDelete.build()).block()

		assertThat(accessor.getGuildById(guildId).block()!!.channels())
			.doesNotContain(Id.of(channelId))
		assertThat(accessor.countChannelsInGuild(guildId).block()!!).isEqualTo(0)
		assertThat(accessor.getChannelsInGuild(guildId).collectList().block())
			.noneMatch { it.id().asLong() == channelId }
	}

	@Test
	fun onChannelDelete_deleteMessagesInChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelCreate = ChannelCreate.builder().channel(channel(channelId).build())
		updater.onChannelCreate(0, channelCreate.build()).block()


		val messageId = generateUniqueSnowflakeId()
		val messageCreate = MessageCreate.builder()
			.message(message(channelId, messageId, generateUniqueSnowflakeId()).build())
			.build()
		updater.onMessageCreate(0, messageCreate).block()
		assertThat(accessor.getMessagesInChannel(channelId).collectList().block()!!)
			.anyMatch { it.id().asLong() == messageId }
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isEqualTo(1)


		val channelDelete = ChannelDelete.builder().channel(channel(channelId).build())
		updater.onChannelDelete(0, channelDelete.build()).block()

		assertThat(accessor.getMessagesInChannel(channelId).collectList().block()).isEmpty()
		assertThat(accessor.countMessagesInChannel(channelId).block()!!).isEqualTo(0)
	}

	@Test
	fun onChannelUpdate_updateChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelCreate = ChannelCreate.builder().channel(channel(channelId).name("Alpha Quadrant").build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		val alphaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(alphaChannel.name().isAbsent).isFalse
		assertThat(alphaChannel.name().get()).isEqualTo("Alpha Quadrant")

		val channelUpdate = ChannelUpdate.builder().channel(channel(channelId).name("Delta Quadrant").build())
		updater.onChannelUpdate(0, channelUpdate.build()).block()

		val deltaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(deltaChannel.name().isAbsent).isFalse
		assertThat(deltaChannel.name().get()).isEqualTo("Delta Quadrant")
	}


	@Test
	fun handleNullByte() {
		val channelId = generateUniqueSnowflakeId()

		val channelCreate = ChannelCreate.builder()
			.channel(channel(channelId).name("everybody gangsta until \u0000").build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		val channel = accessor.getChannelById(channelId).block()!!
		assertThat(channel.id().asLong()).isEqualTo(channelId)
		assertThat(channel.name().isAbsent).isFalse
		assertThat(channel.name().get()).isIn(
			"everybody gangsta until \u0000", // Correct representation
			"everybody gangsta until \uFFFD", // Unicode replacement character
		)
		assertThat(channel.guildId().isAbsent).isTrue

		assertThat(accessor.channels.collectList().block())
			.anyMatch { it.id().asLong() == channelId }
	}


	private val noop = storeLayoutProvider.withFlags(StoreFlag.allBut(StoreFlag.CHANNEL))
	private val noopAccessor = noop.dataAccessor
	private val noopUpdater = noop.gatewayDataUpdater

	@Test
	fun givenNoChannelStoreFlag_countIsZero() {
//		noopAccessor.countChannelsInGuild() TODO with guild
//		noopAccessor.getChannelsInGuild() TODO with guild
		// TODO delete guild
		// TODO delete shard
		assertThat(noopAccessor.countChannels().block()!!).isZero
	}

	@Test
	fun givenNoChannelStoreFlag_channelsIsEmpty() {
		assertThat(noopAccessor.channels.collectList().block()).isEmpty()
	}

	@Test
	fun givenNoChannelStoreFlag_onChannelCreate_doNotCreateChannel() {
		val channelId = generateUniqueSnowflakeId()

		val channelCreate = ChannelCreate.builder()
			.channel(channel(channelId).name("Emergency Medical Holographic Channel").build())
		noopUpdater.onChannelCreate(0, channelCreate.build()).block()

		assertThat(accessor.getChannelById(channelId).blockOptional()).isEmpty
		assertThat(accessor.channels.collectList().block())
			.noneMatch { it.id().asLong() == channelId }

		val channelDelete = ChannelDelete.builder().channel(channel(channelId).build())
		assertThat(noopUpdater.onChannelDelete(0, channelDelete.build()).blockOptional()).isEmpty
		assertThat(updater.onChannelDelete(0, channelDelete.build()).blockOptional()).isEmpty
	}

	@Test
	fun givenNoChannelStoreFlag_onChannelUpdate_doNotCreateChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelUpdate = ChannelUpdate.builder().channel(channel(channelId).name("Alpha Quadrant").build())
		assertThat(noopUpdater.onChannelUpdate(0, channelUpdate.build()).blockOptional()).isEmpty

		assertThat(accessor.getChannelById(channelId).blockOptional()).isEmpty
	}

	@Test
	fun givenNoChannelStoreFlag_onChannelUpdate_doNotUpdateChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelCreate = ChannelCreate.builder().channel(channel(channelId).name("Alpha Quadrant").build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		var alphaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(alphaChannel.name().isAbsent).isFalse
		assertThat(alphaChannel.name().get()).isEqualTo("Alpha Quadrant")

		val channelUpdate = ChannelUpdate.builder().channel(channel(channelId).name("Delta Quadrant").build())
		assertThat(noopUpdater.onChannelUpdate(0, channelUpdate.build()).blockOptional()).isEmpty

		alphaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(alphaChannel.name().isAbsent).isFalse
		assertThat(alphaChannel.name().get()).isEqualTo("Alpha Quadrant")
	}

	@Test
	fun givenNoChannelStoreFlag_onChannelDelete_doNoDeleteChannel() {
		val channelId = generateUniqueSnowflakeId()
		val channelCreate = ChannelCreate.builder().channel(channel(channelId).name("Alpha Quadrant").build())
		updater.onChannelCreate(0, channelCreate.build()).block()

		var alphaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(alphaChannel.name().isAbsent).isFalse
		assertThat(alphaChannel.name().get()).isEqualTo("Alpha Quadrant")

		val channelDelete = ChannelDelete.builder().channel(channel(channelId).build())
		assertThat(noopUpdater.onChannelDelete(0, channelDelete.build()).blockOptional()).isEmpty

		alphaChannel = accessor.getChannelById(channelId).block()!!
		assertThat(alphaChannel.name().isAbsent).isFalse
		assertThat(alphaChannel.name().get()).isEqualTo("Alpha Quadrant")
	}
}
