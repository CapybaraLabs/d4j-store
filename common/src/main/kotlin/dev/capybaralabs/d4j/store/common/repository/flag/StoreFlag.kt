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
		fun all(): EnumSet<StoreFlag> = EnumSet.allOf(StoreFlag::class.java)

		fun none(): EnumSet<StoreFlag> = EnumSet.noneOf(StoreFlag::class.java)

		fun allBut(vararg storeFlags: StoreFlag): EnumSet<StoreFlag> {
			val result = all()
			result.removeAll(storeFlags.asList())
			return result
		}
	}
}
