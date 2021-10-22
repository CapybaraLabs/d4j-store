package dev.capybaralabs.d4j.store.tck

import dev.capybaralabs.d4j.store.common.repository.flag.StoreFlag
import discord4j.common.store.api.layout.StoreLayout
import java.util.EnumSet

interface StoreLayoutProvider {

	fun defaultLayout(): StoreLayout

	fun withFlags(storeFlags: EnumSet<StoreFlag>): StoreLayout

}
