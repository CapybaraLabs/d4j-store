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
import dev.capybaralabs.d4j.store.postgres.repository.ChannelRepository
import dev.capybaralabs.d4j.store.postgres.repository.EmojiRepository
import dev.capybaralabs.d4j.store.postgres.repository.GuildRepository
import dev.capybaralabs.d4j.store.postgres.repository.MemberRepository
import dev.capybaralabs.d4j.store.postgres.repository.MessageRepository
import dev.capybaralabs.d4j.store.postgres.repository.PresenceRepository
import dev.capybaralabs.d4j.store.postgres.repository.Repositories
import dev.capybaralabs.d4j.store.postgres.repository.RoleRepository
import dev.capybaralabs.d4j.store.postgres.repository.UserRepository
import dev.capybaralabs.d4j.store.postgres.repository.VoiceStateRepository
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
 */
class PostgresStoreLayout(connectionFactory: ConnectionFactory) : StoreLayout {


	companion object {
		val log: Logger = LoggerFactory.getLogger(PostgresStoreLayout::class.java)
	}

	private val serde: PostgresSerde = JacksonJsonSerde(objectMapper())

	// TODO implement versioning / migrations
	// TODO avoid blocking calls (db creations) in constructor?
	private val repositories = Repositories(
		connectionFactory,
		ChannelRepository(connectionFactory, serde),
		EmojiRepository(connectionFactory, serde),
		GuildRepository(connectionFactory, serde),
		MemberRepository(connectionFactory, serde),
		MessageRepository(connectionFactory, serde),
		PresenceRepository(connectionFactory, serde),
		RoleRepository(connectionFactory, serde),
		UserRepository(connectionFactory, serde),
		VoiceStateRepository(connectionFactory, serde),
	)

	private val postgresDataAccessor: PostgresDataAccessor = PostgresDataAccessor(repositories)
	private val postgresGatewayDataUpdater: PostgresGatewayDataUpdater = PostgresGatewayDataUpdater(repositories)

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
