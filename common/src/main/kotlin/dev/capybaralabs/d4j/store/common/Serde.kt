package dev.capybaralabs.d4j.store.common

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import discord4j.discordjson.possible.PossibleFilter
import discord4j.discordjson.possible.PossibleModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Serde {

	companion object {
		val log: Logger = LoggerFactory.getLogger(Serde::class.java)

		// Copied over from D4J's RedisStoreDefaults
		fun objectMapper(): ObjectMapper {
			return ObjectMapper()
				.registerModule(PossibleModule())
				.registerModule(Jdk8Module())
				.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
				.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
				.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
				.setDefaultPropertyInclusion(
					JsonInclude.Value.construct(
						JsonInclude.Include.CUSTOM,
						JsonInclude.Include.ALWAYS, PossibleFilter::class.java, null
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
}
