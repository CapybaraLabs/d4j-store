package dev.capybaralabs.d4j.store.postgres

import dev.capybaralabs.d4j.store.common.CommonDataAccessor
import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.common.Serde
import dev.capybaralabs.d4j.store.common.repository.flag.FlaggedRepositories
import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import dev.capybaralabs.d4j.store.postgres.repository.PostgresChannelRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresEmojiRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresGuildRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresMemberRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresMessageRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresPresenceRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresRepositories
import dev.capybaralabs.d4j.store.postgres.repository.PostgresRoleRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresStickerRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresUserRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresVoiceStateRepository
import discord4j.common.store.api.layout.DataAccessor
import discord4j.common.store.api.layout.GatewayDataUpdater
import discord4j.common.store.api.layout.StoreLayout
import io.r2dbc.spi.ConnectionFactory
import java.util.EnumSet

/**
 * TODO
 *
 *
 * The `.flatMap(PostgresqlResult::getRowsUpdated)`
 * lines are necessary because of https://github.com/pgjdbc/r2dbc-postgresql/issues/194#issuecomment-557443260
 *
 *
 * Ids have to saved as TEXT, because JSONB does not support deleting numeric array entries (easily)
 *
 */
class PostgresStoreLayout(
	connectionFactory: ConnectionFactory,
	storeFlags: EnumSet<StoreFlag> = StoreFlag.all(),
) : StoreLayout {

	private val serde: PostgresSerde = JacksonJsonSerde(Serde.objectMapper())

	// TODO implement versioning / migrations
	// TODO avoid blocking calls (db creations) in constructor?
	private val repositories = FlaggedRepositories(
		storeFlags, PostgresRepositories(
			connectionFactory,
			PostgresChannelRepository(connectionFactory, serde),
			PostgresEmojiRepository(connectionFactory, serde),
			PostgresGuildRepository(connectionFactory, serde),
			PostgresMemberRepository(connectionFactory, serde),
			PostgresMessageRepository(connectionFactory, serde),
			PostgresPresenceRepository(connectionFactory, serde),
			PostgresRoleRepository(connectionFactory, serde),
			PostgresStickerRepository(connectionFactory, serde),
			PostgresUserRepository(connectionFactory, serde),
			PostgresVoiceStateRepository(connectionFactory, serde),
		)
	)

	private val postgresDataAccessor: CommonDataAccessor = CommonDataAccessor(repositories)
	private val postgresGatewayDataUpdater: CommonGatewayDataUpdater = CommonGatewayDataUpdater(repositories)

	override fun getDataAccessor(): DataAccessor {
		return postgresDataAccessor
	}

	override fun getGatewayDataUpdater(): GatewayDataUpdater {
		return postgresGatewayDataUpdater
	}

}
