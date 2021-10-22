package dev.capybaralabs.d4j.store.postgres

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import dev.capybaralabs.d4j.store.tck.StoreLayoutProvider
import dev.capybaralabs.d4j.store.tck.StoreLayoutResolver
import dev.capybaralabs.d4j.store.tck.StoreTck
import discord4j.common.store.api.layout.StoreLayout
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import java.util.EnumSet
import reactor.tools.agent.ReactorDebugAgent

private val connectionPool: ConnectionFactory by lazy {
	ReactorDebugAgent.init()
	ReactorDebugAgent.processExistingClasses()
	return@lazy ConnectionPool(
		ConnectionPoolConfiguration
			.builder(ConnectionFactories.get("r2dbc:tc:postgresql:///test?TC_IMAGE_TAG=13"))
			.build()
	)
}

private val storeLayout: StoreLayout by lazy { PostgresStoreLayout(connectionPool) }

class PostgresStoreTck : StoreTck, StoreLayoutResolver(), StoreLayoutProvider {

	override fun storeLayoutProvider(): StoreLayoutProvider {
		return this
	}

	override fun defaultLayout(): StoreLayout {
		return storeLayout
	}

	override fun withFlags(storeFlags: EnumSet<StoreFlag>): StoreLayout {
		return PostgresStoreLayout(connectionPool, storeFlags)
	}
}
