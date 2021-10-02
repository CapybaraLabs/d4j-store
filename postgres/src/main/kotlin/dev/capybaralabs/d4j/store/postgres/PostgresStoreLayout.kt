package dev.capybaralabs.d4j.store.postgres

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY
import com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS
import com.fasterxml.jackson.annotation.JsonInclude.Include.CUSTOM
import com.fasterxml.jackson.annotation.JsonInclude.Value
import com.fasterxml.jackson.annotation.PropertyAccessor.ALL
import com.fasterxml.jackson.annotation.PropertyAccessor.CREATOR
import com.fasterxml.jackson.annotation.PropertyAccessor.GETTER
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import dev.capybaralabs.d4j.store.common.CommonDataAccessor
import dev.capybaralabs.d4j.store.common.CommonGatewayDataUpdater
import dev.capybaralabs.d4j.store.postgres.repository.PostgresChannelRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresEmojiRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresGuildRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresMemberRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresMessageRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresPresenceRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresRepositories
import dev.capybaralabs.d4j.store.postgres.repository.PostgresRoleRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresUserRepository
import dev.capybaralabs.d4j.store.postgres.repository.PostgresVoiceStateRepository
import discord4j.common.store.api.layout.DataAccessor
import discord4j.common.store.api.layout.GatewayDataUpdater
import discord4j.common.store.api.layout.StoreLayout
import discord4j.discordjson.possible.PossibleFilter
import discord4j.discordjson.possible.PossibleModule
import io.r2dbc.spi.ConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
class PostgresStoreLayout(connectionFactory: ConnectionFactory) : StoreLayout {


	companion object {
		val log: Logger = LoggerFactory.getLogger(PostgresStoreLayout::class.java)
	}

	private val serde: PostgresSerde = JacksonJsonSerde(objectMapper())

	// TODO implement versioning / migrations
	// TODO avoid blocking calls (db creations) in constructor?
	private val repositories = PostgresRepositories(
		connectionFactory,
		PostgresChannelRepository(connectionFactory, serde),
		PostgresEmojiRepository(connectionFactory, serde),
		PostgresGuildRepository(connectionFactory, serde),
		PostgresMemberRepository(connectionFactory, serde),
		PostgresMessageRepository(connectionFactory, serde),
		PostgresPresenceRepository(connectionFactory, serde),
		PostgresRoleRepository(connectionFactory, serde),
		PostgresUserRepository(connectionFactory, serde),
		PostgresVoiceStateRepository(connectionFactory, serde),
	)

	private val postgresDataAccessor: CommonDataAccessor = CommonDataAccessor(repositories)
	private val postgresGatewayDataUpdater: CommonGatewayDataUpdater = CommonGatewayDataUpdater(repositories)

	override fun getDataAccessor(): DataAccessor {
		return postgresDataAccessor
	}

	override fun getGatewayDataUpdater(): GatewayDataUpdater {
		return postgresGatewayDataUpdater
	}

	// Copied over from RedisStoreDefaults
	private fun objectMapper(): ObjectMapper {
		return ObjectMapper()
			.registerModule(PossibleModule())
			.registerModule(Jdk8Module())
			.setVisibility(ALL, NONE)
			.setVisibility(GETTER, PUBLIC_ONLY)
			.setVisibility(CREATOR, ANY)
			.setDefaultPropertyInclusion(
				Value.construct(
					CUSTOM,
					ALWAYS, PossibleFilter::class.java, null
				)
			)
			.addHandler(object : DeserializationProblemHandler() {
				override fun handleUnknownProperty(
					ctxt: DeserializationContext, p: JsonParser,
					deserializer: JsonDeserializer<*>, beanOrClass: Any,
					propertyName: String,
				): Boolean {
					log.warn("Unknown property in {}: {}", beanOrClass, propertyName)
					p.skipChildren()
					return true
				}
			})
	}
}
