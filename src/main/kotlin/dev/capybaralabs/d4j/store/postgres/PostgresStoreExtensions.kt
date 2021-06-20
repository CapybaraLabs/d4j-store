package dev.capybaralabs.d4j.store.postgres

import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.ReactionData
import discord4j.discordjson.possible.Possible
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import java.util.Optional
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono


/**
 * Run a closure returning a Mono in the a scope of a database connection, handling the lifecycle (closing the connection)
 */
internal fun <T> withConnection(factory: ConnectionFactory, closure: (Connection) -> Mono<T>): Mono<T> {
	return Mono.usingWhen(
		factory.create(),
		{ closure.invoke(it) },
		{ it.close() }
	)
}

/**
 * Run a closure returning a Flux in the a scope of a database connection, handling the lifecycle (closing the connection)
 */
internal fun <T> withConnectionMany(factory: ConnectionFactory, closure: (Connection) -> Flux<T>): Flux<T> {
	return Flux.usingWhen(
		factory.create(),
		{ closure.invoke(it) },
		{ it.close() }
	)
}

/**
 * Transform a single result row with a "count" column into a Long
 */
internal fun Publisher<out Result>.mapToCount(): Mono<Long> {
	return toMono() // single statement allowed only
		.flatMapMany { it.map { row, _ -> row.get("count", java.lang.Long::class.java) } }
		.toMono() // single row expected
		.map { it.toLong() }
}

// consuming the results is necessary for all queries because of https://github.com/pgjdbc/r2dbc-postgresql/issues/194#issuecomment-557443260
internal fun Flux<out Result>.consume(): Flux<Int> = flatMap { it.rowsUpdated }

// these are a bunch of convenience methods to achieve that
internal fun Statement.executeConsuming(): Flux<Int> = execute().toFlux().consume()
internal fun Statement.executeConsumingSingle(): Mono<Int> = executeConsuming().toMono()


/**
 * Deserialize the data column of multiple rows into a Flux of the requested class using the passed serde
 */
internal fun <E> Publisher<out Result>.deserializeManyFromData(clazz: Class<E>, serde: PostgresSerde): Flux<E> {
	return this.toFlux()
		.flatMap { it.map { row, _ -> row.get("data", ByteArray::class.java) } }
		.map { serde.deserialize(it, clazz) }
}

/**
 * Deserialize the data column of a single rows into a Mono of the requested class using the passed serde
 */
internal fun <E> Publisher<out Result>.deserializeOneFromData(clazz: Class<E>, serde: PostgresSerde): Mono<E> {
	return deserializeManyFromData(clazz, serde).toMono()  // single statement and row expected
}


/**
 * Transform an Optional into a Possible
 */
internal fun <T> Optional<T>.toPossible(): Possible<T> = map { Possible.of(it) }.orElse(Possible.absent())

/**
 * Collapse a nested Possible<Optional<>> into a Possible
 */
internal fun <T> Possible<Optional<T>>.collapse(): Possible<T> = toOptional().flatMap { it }.map { Possible.of(it) }.orElse(Possible.absent())


/**
 * Sum elements distinctly into a list.
 */
internal fun <T> sumDistinct(original: Collection<T>, elements: Collection<T>): List<T> {
	val result = original.toMutableSet()
	result.addAll(elements)
	return result.toList()
}

/**
 * Equality for ReactionData & EmojiData
 */
internal fun ReactionData.equalsEmoji(emojiData: EmojiData): Boolean {
	val emojiHasId = emojiData.id().isPresent
	return emojiHasId && emojiData.id() == this.emoji().id()
		|| !emojiHasId && emojiData.name() == this.emoji().name()
}
