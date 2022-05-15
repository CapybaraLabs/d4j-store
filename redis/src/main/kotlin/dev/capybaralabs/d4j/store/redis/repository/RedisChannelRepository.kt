package dev.capybaralabs.d4j.store.redis.repository

import dev.capybaralabs.d4j.store.common.collectSet
import dev.capybaralabs.d4j.store.common.isPresent
import dev.capybaralabs.d4j.store.common.repository.ChannelRepository
import dev.capybaralabs.d4j.store.redis.RedisFactory
import discord4j.discordjson.json.ChannelData
import org.intellij.lang.annotations.Language
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisElementReader
import org.springframework.data.redis.serializer.RedisElementWriter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

internal class RedisChannelRepository(prefix: String, factory: RedisFactory) : RedisRepository(prefix), ChannelRepository {

	private val channelKey = key("channel")
	private val channelOps = RedisHashOps(channelKey, factory, Long::class.java, ChannelData::class.java)

	private val shardIndexKey = "$channelKey:shard-index"
	private val shardIndex = twoWayIndex(shardIndexKey, factory)

	private val guildIndexKey = "$channelKey:guild-index"
	private val guildIndex = oneWayIndex(guildIndexKey, factory)

	private val gShardIndexKey = "$channelKey:guild-shard-index"
	private val gShardIndex = twoWayIndex(gShardIndexKey, factory)

	private val evalOps = factory.createRedisOperations<String, ChannelData>()
	private val genericWriter = RedisElementWriter.from(factory.genericSerializer<Any>())
	private val longReader = RedisElementReader.from(factory.serializer(Long::class.java))
	private val voidReader = RedisElementReader.from(factory.serializer(Void::class.java))

	override fun save(channel: ChannelData, shardId: Int): Mono<Void> {
		return saveAll(listOf(channel), shardId)
	}

	@Language("Lua")
	private val saveScript = RedisScript.of<Void>(
		"""
			local channelKey = KEYS[1]
			local shardIndexKey = KEYS[2]
			local guildShardIndexKey = KEYS[3]

			local shardId = ARGV[1]
			local channelIds = cjson.decode(ARGV[2])
			local channels = cjson.decode(ARGV[3])
			local guildIds = cjson.decode(ARGV[4])
			local channelIdsByGuildId = cjson.decode(ARGV[5])
			local guildIndexKeyPrefix = ARGV[6]:gsub('"', '') --unfuck quotation marks

			-- addToShardIndex
			local addToShardIndex = {}
			for _, channelId in ipairs(channelIds) do
				table.insert(addToShardIndex, shardId)
				table.insert(addToShardIndex, channelId)
			end
			redis.call('ZADD', shardIndexKey, unpack(addToShardIndex))

			-- addToGuildIndex
			for i, guildId in ipairs(guildIds) do
				local channelIdsInGuild = channelIdsByGuildId[i]
				local guildIndexKey = guildIndexKeyPrefix .. ':' .. guildId
				redis.call('SADD', guildIndexKey, unpack(channelIdsInGuild))
			end

			-- addToGuildShardIndex
			local addToGuildShardIndex = {}
			for _, guildId in ipairs(guildIds) do
				table.insert(addToGuildShardIndex, shardId)
				table.insert(addToGuildShardIndex, guildId)
			end
			if next(addToGuildShardIndex) ~= nil then
				redis.call('ZADD', guildShardIndexKey, unpack(addToGuildShardIndex))
			end

			-- saveChannels
			local saveChannels = {}
			for i, channelId in ipairs(channelIds) do
				local channel = channels[i]
				table.insert(saveChannels, channelId)
				table.insert(saveChannels, cjson.encode(channel))
			end

			redis.call('HSET', channelKey, unpack(saveChannels))
			return redis.status_reply('OK')
		""".trimIndent()
	)

	override fun saveAll(channels: List<ChannelData>, shardId: Int): Mono<Void> {
		if (channels.isEmpty()) {
			return Mono.empty()
		}

		// longs need to be strings to avoid lua double number fuckery
		val channelIds = channels.map { it.id().asString() }
		val channelIdsByGuildIds = channels
			.filter { it.guildId().isPresent() }
			.groupBy { it.guildId().get().asString() }
			.mapValues { entry -> entry.value.map { it.id().asString() } }

		val guildIds = ArrayList<String>()
		val nestedChannelIds = ArrayList<List<String>>()
		for (entry in channelIdsByGuildIds) {
			guildIds.add(entry.key)
			nestedChannelIds.add(entry.value)
		}

		return evalOps.execute(
			saveScript,
			listOf(channelKey, shardIndexKey, gShardIndexKey),
			listOf(shardId, channelIds, channels, guildIds, nestedChannelIds, guildIndexKey),
			genericWriter,
			voidReader,
		).toMono()
	}

	@Language("Lua")
	private val deleteScript = RedisScript.of(
		"""
			local channelKey = KEYS[1]
			local shardIndexKey = KEYS[2]
			local guildIndexKey = KEYS[3] --nullable
			local channelId = ARGV[1]
			-- shardIndex
			redis.call('ZREM', shardIndexKey, channelId)

			-- guildIndex
			if guildIndexKey then
				redis.call('SREM', guildIndexKey, channelId)
			end

			-- channel
			return redis.call('HDEL', channelKey, channelId)
		""".trimIndent(), Long::class.java
	)

	override fun delete(channelId: Long, guildId: Long?): Mono<Long> {
		return evalOps.execute(
			deleteScript,
			listOfNotNull(channelKey, shardIndexKey, guildId?.let { guildIndex.key(it) }),
			listOf(channelId),
			genericWriter,
			longReader,
		).last()
	}

	override fun deleteByGuildId(guildId: Long): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { channelIdsInGuild ->
					val removeChannels = channelOps.remove(*channelIdsInGuild.toTypedArray())

					val removeFromShardIndex = shardIndex.removeElements(*channelIdsInGuild.toTypedArray())
					val deleteGuildIndexEntry = guildIndex.deleteGroup(guildId)
					val removeGuildFromShardIndex = gShardIndex.removeElements(guildId)

					Mono.`when`(removeFromShardIndex, deleteGuildIndexEntry, removeGuildFromShardIndex)
						.then(removeChannels)
				}
		}
	}

	override fun deleteByShardId(shardId: Int): Mono<Long> {
		// TODO consider LUA script for atomicity
		return Mono.defer {
			val getChannelIds: Mono<Set<Long>> = shardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { shardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }
			val getGuildIds: Mono<Set<Long>> = gShardIndex.getElementsByGroup(shardId).collectSet()
				.flatMap { gShardIndex.deleteByGroupId(shardId).then(Mono.just(it)) }

			val getAllChannelIds = getGuildIds.flatMap { guildIds ->
				// Technically we don't need to fetch the channel ids of the groups here, we can rely on the shard index only.
				guildIndex.getElementsInGroups(guildIds).collectSet()
					.flatMap { guildIndex.deleteGroups(guildIds).then(Mono.just(it)) }
					.flatMap { idsInGuilds -> getChannelIds.map { channelIds -> idsInGuilds + channelIds } }
			}

			getAllChannelIds.flatMap { channelOps.remove(*it.toTypedArray()) }
		}
	}


	override fun countChannels(): Mono<Long> {
		return Mono.defer {
			channelOps.size()
		}
	}

	override fun countChannelsInGuild(guildId: Long): Mono<Long> {
		return Mono.defer {
			guildIndex.countElementsInGroup(guildId)
		}
	}


	override fun getChannelById(channelId: Long): Mono<ChannelData> {
		return Mono.defer {
			channelOps.get(channelId)
		}
	}

	override fun getChannels(): Flux<ChannelData> {
		return Flux.defer {
			channelOps.values()
		}
	}

	override fun getChannelsInGuild(guildId: Long): Flux<ChannelData> {
		return Flux.defer {
			guildIndex.getElementsInGroup(guildId).collectList()
				.flatMap { channelOps.multiGet(it) }
				.flatMapMany { Flux.fromIterable(it) }
		}
	}
}
