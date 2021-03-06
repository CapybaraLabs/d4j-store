package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import discord4j.discordjson.json.gateway.GuildCreate
import discord4j.discordjson.json.gateway.GuildEmojisUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EmojiTest(storeLayoutProvider: StoreLayoutProvider) {

	private val storeLayout = storeLayoutProvider.defaultLayout()
	private val accessor = storeLayout.dataAccessor
	private val updater = storeLayout.gatewayDataUpdater

	@Test
	fun countEmojis() {
		assertThat(accessor.countEmojis().blockOptional()).isPresent
	}


	@Test
	fun onGuildEmojisUpdate_addEmoji() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val emojiIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).isNull()
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).isNull()
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(2)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.noneMatch { it.asLong() == emojiIdC }
			.noneMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdA).name("emojiA").build(),
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC").build(),
				emoji(emojiIdD).name("emojiD").build(),
			)
			.build()

		updater.onGuildEmojisUpdate(0, emojisUpdate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).matches { it.name().get() == "emojiD" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(4)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.anyMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
	}

	@Test
	fun onGuildEmojisUpdate_removeEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val emojiIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
						emoji(emojiIdC).name("emojiC").build(),
						emoji(emojiIdD).name("emojiD").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).matches { it.name().get() == "emojiD" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(4)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.anyMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC").build(),
			)
			.build()

		updater.onGuildEmojisUpdate(0, emojisUpdate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).isNull()
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).isNull()
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(2)
			.noneMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.noneMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(2)
			.noneMatch { it.id().get().asLong() == emojiIdA }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.noneMatch { it.id().get().asLong() == emojiIdD }
		assertThat(accessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiIdA }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.noneMatch { it.id().get().asLong() == emojiIdD }
	}

	@Test
	fun onGuildEmojisUpdate_updateEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
						emoji(emojiIdC).name("emojiC").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(3)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdA).name("emojiA_updated").build(),
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC_updated").build(),
			)
			.build()

		updater.onGuildEmojisUpdate(0, emojisUpdate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA_updated" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC_updated" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(3)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA_updated" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC_updated" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA_updated" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC_updated" }
	}


	private val noop = storeLayoutProvider.withFlags(StoreFlag.allBut(StoreFlag.EMOJI))
	private val noopAccessor = noop.dataAccessor
	private val noopUpdater = noop.gatewayDataUpdater


	@Test
	fun givenNoEmojiStoreFlag_countIsZero() {
//		noopAccessor.countEmojisInGuild() TODO with guild
//		noopAccessor.getEmojisInGuild() TODO with guild
		// TODO delete guild
		// TODO delete shard
		assertThat(noopAccessor.countEmojis().block()!!).isZero
	}

	@Test
	fun givenNoEmojiStoreFlag_channelsIsEmpty() {
		assertThat(noopAccessor.emojis.collectList().block()).isEmpty()
	}

	@Test
	fun givenNoEmojiStoreFlag_onGuildEmojisUpdate_doNotAddEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val emojiIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).isNull()
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).isNull()
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(2)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.noneMatch { it.asLong() == emojiIdC }
			.noneMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdA).name("emojiA").build(),
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC").build(),
				emoji(emojiIdD).name("emojiD").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildEmojisUpdate(0, emojisUpdate).block()).isEmpty()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).isNull()
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdA).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdB).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdC).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdD).block()).isNull()

		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(2)
		assertThat(noopAccessor.countEmojisInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(4) // only Emoji store is noop, guild still gets the update
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.anyMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(2)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
		assertThat(noopAccessor.getEmojisInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
		assertThat(noopAccessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiIdA }
			.noneMatch { it.id().get().asLong() == emojiIdB }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
	}

	@Test
	fun givenNoEmojiStoreFlag_onGuildEmojisUpdate_doNotRemoveEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val emojiIdD = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
						emoji(emojiIdC).name("emojiC").build(),
						emoji(emojiIdD).name("emojiD").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).matches { it.name().get() == "emojiD" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(4)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.anyMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildEmojisUpdate(0, emojisUpdate).block()).isEmpty()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.getEmojiById(guildId, emojiIdD).block()).matches { it.name().get() == "emojiD" }
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdA).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdB).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdC).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdD).block()).isNull()

		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(4)
		assertThat(noopAccessor.countEmojisInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(2) // only Emoji store is noop, guild still gets the update
			.noneMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
			.noneMatch { it.asLong() == emojiIdD }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(4)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
		assertThat(noopAccessor.getEmojisInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
			.anyMatch { it.id().get().asLong() == emojiIdD && it.name().get() == "emojiD" }
		assertThat(noopAccessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiIdA }
			.noneMatch { it.id().get().asLong() == emojiIdB }
			.noneMatch { it.id().get().asLong() == emojiIdC }
			.noneMatch { it.id().get().asLong() == emojiIdD }
	}

	@Test
	fun givenNoEmojiStoreFlag_onGuildEmojisUpdate_doNotUpdateEmojis() {
		val guildId = generateUniqueSnowflakeId()
		val emojiIdA = generateUniqueSnowflakeId()
		val emojiIdB = generateUniqueSnowflakeId()
		val emojiIdC = generateUniqueSnowflakeId()
		val guildCreate = GuildCreate.builder()
			.guild(
				guild(guildId)
					.addEmojis(
						emoji(emojiIdA).name("emojiA").build(),
						emoji(emojiIdB).name("emojiB").build(),
						emoji(emojiIdC).name("emojiC").build(),
					)
					.build()
			)
			.build()


		updater.onGuildCreate(0, guildCreate).block()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(3)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }


		val emojisUpdate = GuildEmojisUpdate.builder()
			.guildId(guildId)
			.addEmojis(
				emoji(emojiIdA).name("emojiA_updated").build(),
				emoji(emojiIdB).name("emojiB").build(),
				emoji(emojiIdC).name("emojiC_updated").build(),
			)
			.build()

		assertThat(noopUpdater.onGuildEmojisUpdate(0, emojisUpdate).block()).isEmpty()

		assertThat(accessor.getEmojiById(guildId, emojiIdA).block()).matches { it.name().get() == "emojiA" }
		assertThat(accessor.getEmojiById(guildId, emojiIdB).block()).matches { it.name().get() == "emojiB" }
		assertThat(accessor.getEmojiById(guildId, emojiIdC).block()).matches { it.name().get() == "emojiC" }
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdA).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdB).block()).isNull()
		assertThat(noopAccessor.getEmojiById(guildId, emojiIdC).block()).isNull()

		assertThat(accessor.countEmojisInGuild(guildId).block()!!).isEqualTo(3)
		assertThat(noopAccessor.countEmojisInGuild(guildId).block()!!).isZero

		assertThat(accessor.getGuildById(guildId).block()!!.emojis())
			.hasSize(3)
			.anyMatch { it.asLong() == emojiIdA }
			.anyMatch { it.asLong() == emojiIdB }
			.anyMatch { it.asLong() == emojiIdC }
		assertThat(accessor.getEmojisInGuild(guildId).collectList().block())
			.hasSize(3)
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
		assertThat(noopAccessor.getEmojisInGuild(guildId).collectList().block()).isEmpty()

		assertThat(accessor.emojis.collectList().block())
			.anyMatch { it.id().get().asLong() == emojiIdA && it.name().get() == "emojiA" }
			.anyMatch { it.id().get().asLong() == emojiIdB && it.name().get() == "emojiB" }
			.anyMatch { it.id().get().asLong() == emojiIdC && it.name().get() == "emojiC" }
		assertThat(noopAccessor.emojis.collectList().block())
			.noneMatch { it.id().get().asLong() == emojiIdA }
			.noneMatch { it.id().get().asLong() == emojiIdB }
			.noneMatch { it.id().get().asLong() == emojiIdC }
	}
}
