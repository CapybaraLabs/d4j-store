package dev.capybaralabs.d4j.store.common

import discord4j.discordjson.Id
import discord4j.discordjson.json.EmojiData
import discord4j.discordjson.json.GuildCreateData
import discord4j.discordjson.json.GuildData
import discord4j.discordjson.json.GuildUpdateData
import discord4j.discordjson.json.ImmutableGuildCreateData
import discord4j.discordjson.json.ImmutableGuildUpdateData
import discord4j.discordjson.json.ReactionData
import discord4j.discordjson.json.StickerData
import discord4j.discordjson.possible.Possible
import java.util.Optional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Collapse a nested Possible<Optional<>> into a Possible
 */
internal fun <T> Possible<Optional<T>>.collapse(): Possible<T> =
	toOptional().flatMap { it }.map { Possible.of(it) }.orElse(Possible.absent())


/**
 * Sum elements distinctly into a list.
 */
internal fun <T> sumDistinct(original: Collection<T>, elements: Collection<T>): List<T> {
	val result = original.toMutableSet()
	result.addAll(elements)
	return result.toList()
}

/**
 * Equality for ReactionData & EmojiData
 */
internal fun ReactionData.equalsEmoji(emojiData: EmojiData): Boolean {
	val emojiHasId = emojiData.id().isPresent
	return emojiHasId && emojiData.id() == this.emoji().id()
		|| !emojiHasId && emojiData.name() == this.emoji().name()
}


fun <T> Possible<T>.isPresent(): Boolean {
	return !isAbsent
}

fun <T> Possible<T>.orElse(other: T): T {
	return toOptional().orElse(other)
}

fun <T> Flux<T>.collectSet(): Mono<Set<T>> = collectList().map { it.toSet() }

fun <N : Number> Mono<N>.toLong(): Mono<Long> = map { it.toLong() }

fun GuildData.stickersOrEmpty(): List<Id> {
	return stickers().orElse(listOf())
}

fun GuildCreateData.stickersOrEmpty(): List<StickerData> {
	return stickers().orElse(listOf())
}

fun GuildUpdateData.stickersOrEmpty(): List<StickerData> {
	return stickers().orElse(listOf())
}

fun ImmutableGuildCreateData.Builder.addStickers(vararg stickerData: StickerData): ImmutableGuildCreateData.Builder {
	return addAllStickers(stickerData.toList())
}

fun ImmutableGuildUpdateData.Builder.addStickers(vararg stickerData: StickerData): ImmutableGuildUpdateData.Builder {
	return addAllStickers(stickerData.toList())
}

fun StickerData.withGuildId(guildId: Long): StickerData {
	return StickerData.builder()
		.from(this)
		.guildId(guildId)
		.build()
}
