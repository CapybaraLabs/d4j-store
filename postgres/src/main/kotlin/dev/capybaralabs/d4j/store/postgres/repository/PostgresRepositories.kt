package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.common.repository.Repositories
import dev.capybaralabs.d4j.store.common.repository.noop.NoopStickerRepository
import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Mono

internal class PostgresRepositories internal constructor(
	private val factory: ConnectionFactory,
	override val channels: PostgresChannelRepository,
	override val emojis: PostgresEmojiRepository,
	override val guilds: PostgresGuildRepository,
	override val members: PostgresMemberRepository,
	override val messages: PostgresMessageRepository,
	override val presences: PostgresPresenceRepository,
	override val roles: PostgresRoleRepository,
	override val stickers: NoopStickerRepository,
	override val users: PostgresUserRepository,
	override val voiceStates: PostgresVoiceStateRepository,
) : Repositories {

	// TODO find a way to safely write across multiple tables
	override fun deleteOrphanedUsers(shardId: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory, "PostgresRepositories.deleteOrphanedUsers") { connection ->
				connection.createStatement(
					"""
					WITH onThisShard AS (
						SELECT user_id
						FROM d4j_discord_member
						WHERE shard_index = $1
					), onOtherShards AS (
						SELECT user_id
						FROM d4j_discord_member
						WHERE shard_index != $1 AND user_id IN (SELECT user_id FROM onThisShard)
					)
					DELETE FROM d4j_discord_user
						WHERE user_id IN (SELECT user_id FROM onThisShard) AND user_id NOT IN (SELECT user_id FROM onOtherShards)
					""".trimIndent()
				)
					.bind("$1", shardId)
					.execute().mapToCount()
			}
		}
	}
}
