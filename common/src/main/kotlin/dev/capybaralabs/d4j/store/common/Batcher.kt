package dev.capybaralabs.d4j.store.common

import java.time.Duration
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2

class Batcher<T>(private val shardId: Int, private val batchMethod: (List<Tuple2<T, Sinks.Empty<Void>>>) -> Unit) {

	private val batchScheduler = Schedulers.newSingle("batcher-shard-$shardId")

	companion object {
		val log: Logger = LoggerFactory.getLogger(Batcher::class.java)
	}

	private val batchSize = 256 // TODO tune
	private val maxIdleDrainPeriod = Duration.ofSeconds(1) // TODO tune
	private val queue: BlockingQueue<Tuple2<T, Sinks.Empty<Void>>> = LinkedBlockingQueue()
	private var lastQueued: Instant = Instant.now()

	init {
		Flux.interval(maxIdleDrainPeriod)
			.publishOn(batchScheduler)
			.doOnNext { drainIfNecessary() }
			.doOnError { error -> log.error("Batcher for shard $shardId errored", error) }
			.retry()
			.subscribe()
	}

	fun queue(element: Tuple2<T, Sinks.Empty<Void>>) {
		queue.add(element)
		lastQueued = Instant.now()
		drainIfNecessary()
	}

	private fun drainIfNecessary() {
		if (queue.isEmpty()) {
			return
		}
		if (queue.size >= batchSize || lastQueued.plus(maxIdleDrainPeriod).isBefore(Instant.now())) {
			val batch = ArrayList<Tuple2<T, Sinks.Empty<Void>>>()
			queue.drainTo(batch)
			if (batch.isNotEmpty()) {
				log.debug("Batch on shard $shardId executing with ${batch.size} elements")
				batchMethod.invoke(batch)
			}
		}
	}
}
