package dev.capybaralabs.d4j.store.postgres.repository

import dev.capybaralabs.d4j.store.postgres.mapToCount
import dev.capybaralabs.d4j.store.postgres.withConnection
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Mono

internal class Repositories internal constructor(
	private val factory: ConnectionFactory,
	internal val channels: ChannelRepository,
	internal val emojis: EmojiRepository,
	internal val guilds: GuildRepository,
	internal val members: MemberRepository,
	internal val messages: MessageRepository,
	internal val presences: PresenceRepository,
	internal val roles: RoleRepository,
	internal val users: UserRepository,
	internal val voiceStates: VoiceStateRepository,
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
