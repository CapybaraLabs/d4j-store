package dev.capybaralabs.d4j.store.common

import java.time.Duration
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.function.Tuple2

class Batcher<T>(private val shardId: Int, private val batchMethod: (List<Tuple2<T, Sinks.Empty<Void>>>) -> Unit) {

	companion object {
		val log: Logger = LoggerFactory.getLogger(Batcher::class.java)
	}

	private val batchSize = 100 // TODO tune
	private val maxDrainPeriod = Duration.ofSeconds(10) // TODO tune
	private val queue: BlockingQueue<Tuple2<T, Sinks.Empty<Void>>> = LinkedBlockingQueue()
	private var lastDrained: Instant = Instant.now()

	init {
		Flux.interval(maxDrainPeriod)
			.doOnNext { drainIfNecessary() }
			.doOnError { error -> log.error("Batcher for shard $shardId errored", error) }
			.retry()
			.subscribe()
	}

	fun append(element: Tuple2<T, Sinks.Empty<Void>>) {
		queue.add(element)
		drainIfNecessary()
	}

	private fun drainIfNecessary() {
		if (queue.size >= batchSize || lastDrained.plus(maxDrainPeriod).isBefore(Instant.now())) {
			val batch = ArrayList<Tuple2<T, Sinks.Empty<Void>>>()
			queue.drainTo(batch)
			if (batch.isNotEmpty()) {
				batchMethod.invoke(batch)
			}
		}
	}
}
