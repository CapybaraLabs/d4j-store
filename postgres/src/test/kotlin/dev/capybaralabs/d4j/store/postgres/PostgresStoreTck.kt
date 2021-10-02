package dev.capybaralabs.d4j.store.postgres

import dev.capybaralabs.d4j.store.tck.StoreLayoutResolver
import dev.capybaralabs.d4j.store.tck.StoreTck
import discord4j.common.store.api.layout.StoreLayout
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import reactor.tools.agent.ReactorDebugAgent

private val storeLayout = PostgresStoreLayout(connectionPool())

private fun connectionPool(): ConnectionPool {
	ReactorDebugAgent.init()
	ReactorDebugAgent.processExistingClasses()
	return ConnectionPool(
		ConnectionPoolConfiguration
			.builder(ConnectionFactories.get("r2dbc:tc:postgresql:///test?TC_IMAGE_TAG=13"))
			.build()
	)
}

class PostgresStoreTck : StoreTck, StoreLayoutResolver() {

	override fun storeLayout(): StoreLayout {
		return storeLayout
	}
}
