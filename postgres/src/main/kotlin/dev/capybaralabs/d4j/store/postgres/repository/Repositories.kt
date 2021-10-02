package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Mono

internal class Repositories internal constructor(
	private val factory: ConnectionFactory,
	internal val channels: PostgresChannelRepository,
	internal val emojis: PostgresEmojiRepository,
	internal val guilds: PostgresGuildRepository,
	internal val members: PostgresMemberRepository,
	internal val messages: PostgresMessageRepository,
	internal val presences: PostgresPresenceRepository,
	internal val roles: PostgresRoleRepository,
	internal val users: PostgresUserRepository,
	internal val voiceStates: PostgresVoiceStateRepository,
) {

	// TODO find a way to safely write across multiple tables
	fun deleteOrphanedUsers(shardIndex: Int): Mono<Long> {
		return Mono.defer {
			withConnection(factory) { connection ->
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
					.bind("$1", shardIndex)
					.execute().mapToCount()
			}
		}
	}
}
