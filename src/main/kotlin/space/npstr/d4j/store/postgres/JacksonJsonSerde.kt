package space.npstr.d4j.store.postgres

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * TODO define proper exceptions
 */
// TODO object mapper needs to write out IDs/Longs as strings, probably
class JacksonJsonSerde(private val mapper: ObjectMapper) : PostgresSerde {

    override fun <I> serialize(value: I): ByteArray {
        try {
            return mapper.writeValueAsBytes(value)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Unable to write JSON for class ${value!!::class.simpleName}: $value", e)
        }
    }

    override fun <I> deserialize(out: ByteArray, clazz: Class<I>): I {
        return try {
            mapper.readValue(out, clazz)
        } catch (e: IOException) {
            throw RuntimeException("Unable to read JSON: ${String(out, StandardCharsets.UTF_8)}", e)
        }
    }
}
