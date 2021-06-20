package dev.capybaralabs.d4j.store.postgres

import java.nio.charset.StandardCharsets

internal interface PostgresSerde {

    fun <I> serialize(value: I): ByteArray

    fun <I> serializeToString(value: I): String {
        return String(serialize(value), StandardCharsets.UTF_8)
    }

    fun <I> deserialize(out: ByteArray, clazz: Class<I>): I

    fun <I> deserializeFromString(out: String, clazz: Class<I>): I {
        return deserialize(out.toByteArray(StandardCharsets.UTF_8), clazz)
    }
}
