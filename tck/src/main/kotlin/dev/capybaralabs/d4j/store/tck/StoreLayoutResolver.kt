package dev.capybaralabs.d4j.store.tck

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

abstract class StoreLayoutResolver : TypeBasedParameterResolver<StoreLayoutProvider>() {

	override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): StoreLayoutProvider {
		return storeLayoutProvider()
	}

	abstract fun storeLayoutProvider(): StoreLayoutProvider
}
