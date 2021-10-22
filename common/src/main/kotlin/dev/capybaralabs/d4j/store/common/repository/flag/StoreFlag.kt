package dev.capybaralabs.d4j.store.common.repository.flag

import java.util.EnumSet

/**
 * Flag for signaling which stores should be used / no-op'd
 */
enum class StoreFlag {
	CHANNEL,
	EMOJI,
	GUILD,
	MEMBER,
	MESSAGE,
	PRESENCE,
	ROLE,
	USER,
	VOICE_STATE,
	//
	;

	companion object {
		val all: EnumSet<StoreFlag>
			get() = EnumSet.allOf(StoreFlag::class.java)

		val none: EnumSet<StoreFlag>
			get() = EnumSet.noneOf(StoreFlag::class.java)
	}
}
