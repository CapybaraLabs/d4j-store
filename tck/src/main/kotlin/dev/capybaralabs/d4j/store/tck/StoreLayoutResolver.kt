package dev.capybaralabs.d4j.store.tck

import discord4j.common.store.api.layout.StoreLayout
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

abstract class StoreLayoutResolver : TypeBasedParameterResolver<StoreLayout>() {

	override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): StoreLayout {
		return storeLayout()
	}

	abstract fun storeLayout(): StoreLayout
}
