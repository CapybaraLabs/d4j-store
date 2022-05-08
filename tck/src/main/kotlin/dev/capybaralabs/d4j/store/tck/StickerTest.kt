package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.addStickers
import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import dev.capybaralabs.d4j.store.common.stickersOrEmpty
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildStickersUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StickerTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun countStickers() {
		assertThat(accessor.countStickers().blockOptional()).isPresent
	}


	@Test
	fun onGuildStickersUpdate_addSticker() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val stickerIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).isNull()
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).isNull()
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(2)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.noneMatch { it.asLong() == stickerIdC }
			.noneMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdA).name("stickerA").build(),
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC").build(),
				sticker(stickerIdD).name("stickerD").build(),
			)
			.build()

		updater.onGuildStickersUpdate(0, stickersUpdate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).matches { it.name() == "stickerD" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(4)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.anyMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
	}

	@Test
	fun onGuildStickersUpdate_removeStickers() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val stickerIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
						sticker(stickerIdC).name("stickerC").build(),
						sticker(stickerIdD).name("stickerD").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).matches { it.name() == "stickerD" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(4)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.anyMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC").build(),
			)
			.build()

		updater.onGuildStickersUpdate(0, stickersUpdate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).isNull()
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).isNull()
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(2)
			.noneMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.noneMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(2)
			.noneMatch { it.id().asLong() == stickerIdA }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.noneMatch { it.id().asLong() == stickerIdD }
		assertThat(accessor.stickers.collectList().block())
			.noneMatch { it.id().asLong() == stickerIdA }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.noneMatch { it.id().asLong() == stickerIdD }
	}

	@Test
	fun onGuildStickersUpdate_updateStickers() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
						sticker(stickerIdC).name("stickerC").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(3)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdA).name("stickerA_updated").build(),
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC_updated").build(),
			)
			.build()

		updater.onGuildStickersUpdate(0, stickersUpdate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA_updated" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC_updated" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(3)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA_updated" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC_updated" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA_updated" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC_updated" }
	}


	private val noop = storeLayoutProvider.withFlags(StoreFlag.allBut(StoreFlag.STICKER))
	private val noopAccessor = noop.dataAccessor
	private val noopUpdater = noop.gatewayDataUpdater


	@Test
	fun givenNoStickerStoreFlag_countIsZero() {
//		noopAccessor.countStickersInGuild() TODO with guild
//		noopAccessor.getStickersInGuild() TODO with guild
		// TODO delete guild
		// TODO delete shard
		assertThat(noopAccessor.countStickers().block()!!).isZero
	}

	@Test
	fun givenNoStickerStoreFlag_channelsIsEmpty() {
		assertThat(noopAccessor.stickers.collectList().block()).isEmpty()
	}

	@Test
	fun givenNoStickerStoreFlag_onGuildStickersUpdate_doNotAddStickers() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val stickerIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).isNull()
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).isNull()
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(2)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.noneMatch { it.asLong() == stickerIdC }
			.noneMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdA).name("stickerA").build(),
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC").build(),
				sticker(stickerIdD).name("stickerD").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildStickersUpdate(0, stickersUpdate).block()).isEmpty()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).isNull()
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdA).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdB).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdC).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdD).block()).isNull()

		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(noopAccessor.countStickersInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(4) // only Sticker store is noop, guild still gets the update
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.anyMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
		assertThat(noopAccessor.getStickersInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
		assertThat(noopAccessor.stickers.collectList().block())
			.noneMatch { it.id().asLong() == stickerIdA }
			.noneMatch { it.id().asLong() == stickerIdB }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
	}

	@Test
	fun givenNoStickerStoreFlag_onGuildStickersUpdate_doNotRemoveStickers() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val stickerIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
						sticker(stickerIdC).name("stickerC").build(),
						sticker(stickerIdD).name("stickerD").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).matches { it.name() == "stickerD" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(4)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.anyMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildStickersUpdate(0, stickersUpdate).block()).isEmpty()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.getStickerById(guildId, stickerIdD).block()).matches { it.name() == "stickerD" }
		assertThat(noopAccessor.getStickerById(guildId, stickerIdA).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdB).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdC).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdD).block()).isNull()

		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(noopAccessor.countStickersInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(2) // only Sticker store is noop, guild still gets the update
			.noneMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
			.noneMatch { it.asLong() == stickerIdD }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
		assertThat(noopAccessor.getStickersInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
			.anyMatch { it.id().asLong() == stickerIdD && it.name() == "stickerD" }
		assertThat(noopAccessor.stickers.collectList().block())
			.noneMatch { it.id().asLong() == stickerIdA }
			.noneMatch { it.id().asLong() == stickerIdB }
			.noneMatch { it.id().asLong() == stickerIdC }
			.noneMatch { it.id().asLong() == stickerIdD }
	}

	@Test
	fun givenNoStickerStoreFlag_onGuildStickersUpdate_doNotUpdateStickers() {
		val guildId = generateUniqueSnowflakeId()
		val stickerIdA = generateUniqueSnowflakeId()
		val stickerIdB = generateUniqueSnowflakeId()
		val stickerIdC = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addStickers(
						sticker(stickerIdA).name("stickerA").build(),
						sticker(stickerIdB).name("stickerB").build(),
						sticker(stickerIdC).name("stickerC").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(3)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }


		val stickersUpdate = GuildStickersUpdate.builder()
			.guildId(guildId)
			.addStickers(
				sticker(stickerIdA).name("stickerA_updated").build(),
				sticker(stickerIdB).name("stickerB").build(),
				sticker(stickerIdC).name("stickerC_updated").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildStickersUpdate(0, stickersUpdate).block()).isEmpty()

		assertThat(accessor.getStickerById(guildId, stickerIdA).block()).matches { it.name() == "stickerA" }
		assertThat(accessor.getStickerById(guildId, stickerIdB).block()).matches { it.name() == "stickerB" }
		assertThat(accessor.getStickerById(guildId, stickerIdC).block()).matches { it.name() == "stickerC" }
		assertThat(noopAccessor.getStickerById(guildId, stickerIdA).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdB).block()).isNull()
		assertThat(noopAccessor.getStickerById(guildId, stickerIdC).block()).isNull()

		assertThat(accessor.countStickersInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(noopAccessor.countStickersInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.stickersOrEmpty())
			.hasSize(3)
			.anyMatch { it.asLong() == stickerIdA }
			.anyMatch { it.asLong() == stickerIdB }
			.anyMatch { it.asLong() == stickerIdC }
		assertThat(accessor.getStickersInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
		assertThat(noopAccessor.getStickersInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.stickers.collectList().block())
			.anyMatch { it.id().asLong() == stickerIdA && it.name() == "stickerA" }
			.anyMatch { it.id().asLong() == stickerIdB && it.name() == "stickerB" }
			.anyMatch { it.id().asLong() == stickerIdC && it.name() == "stickerC" }
		assertThat(noopAccessor.stickers.collectList().block())
			.noneMatch { it.id().asLong() == stickerIdA }
			.noneMatch { it.id().asLong() == stickerIdB }
			.noneMatch { it.id().asLong() == stickerIdC }
	}
}
