package dev.capybaralabs.d4j.store.postgres

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import java.util.concurrent.atomic.AtomicLong
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono


/**
 * Run a closure returning a Mono in the a scope of a database connection, handling the lifecycle (closing the connection)
 */
internal fun <T> withConnection(factory: ConnectionFactory, operation: String, closure: (Connection) -> Mono<T>): Mono<T> {
	return instrumented(Mono.usingWhen(
		factory.create(),
		{ closure.invoke(it) },
		{ it.close() }
	), operation)
}

/**
 * Run a closure returning a Flux in the a scope of a database connection, handling the lifecycle (closing the connection)
 */
internal fun <T> withConnectionMany(factory: ConnectionFactory, operation: String, closure: (Connection) -> Flux<T>): Flux<T> {
	return instrumented(Flux.usingWhen(
		factory.create(),
		{ closure.invoke(it) },
		{ it.close() }
	), operation)
}

internal fun <T> instrumented(mono: Mono<T>, operation: String): Mono<T> {
	val subscribeTimeHolder = AtomicLong()
	return mono
		.doOnSubscribe { subscribeTimeHolder.set(System.nanoTime()) }
		.doOnTerminate { doInstrument(subscribeTimeHolder.get(), operation) }
}

internal fun <T> instrumented(flux: Flux<T>, operation: String): Flux<T> {
	val subscribeTimeHolder = AtomicLong()
	return flux
		.doOnSubscribe { subscribeTimeHolder.set(System.nanoTime()) }
		.doOnTerminate { doInstrument(subscribeTimeHolder.get(), operation) }
}


private val log = LoggerFactory.getLogger("dev.capybaralabs.d4j.store.postgres.PostgresStoreExtensions")
private const val NANOSECONDS_PER_MILLISECOND = 1000000.0

private fun doInstrument(startTime: Long, operation: String) {
	if (startTime == 0L) {
		log.warn("No subscribe time present for operation {}", operation)
		return
	}
	val nanos = System.nanoTime() - startTime
	val millis: Double = nanos / NANOSECONDS_PER_MILLISECOND
	log.trace("{} took {}ms", operation, millis)
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
internal fun Statement.executeConsumingAll(): Flux<Int> = execute().toFlux().consume()
internal fun Statement.executeConsumingSingle(): Mono<Int> = executeConsumingAll().toMono()


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
