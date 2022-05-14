package dev.capybaralabs.d4j.store.common.repository.flag

import dev.capybaralabs.d4j.store.common.repository.MemberRepository
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.UserData
import reactor.core.publisher.Mono

class MinimalUserMemberRepository(private val delegate: MemberRepository) : MemberRepository by delegate {

	override fun save(guildId: Long, member: MemberData, shardId: Int): Mono<Void> {
		return delegate.save(guildId, minimalData(member), shardId)
	}

	override fun saveAll(membersByGuild: Map<Long, List<MemberData>>, shardId: Int): Mono<Void> {
		val minimalData = membersByGuild.mapValues { it.value.map { p -> minimalData(p) } }
		return saveAll(minimalData, shardId)
	}


	private fun minimalData(member: MemberData): MemberData {
		val minimalUserData = UserData.builder()
			.id(member.user().id())
			.username(member.user().username())
			.discriminator(member.user().discriminator())
			.avatar(member.user().avatar())
			.build();

		return MemberData.builder()
			.from(member)
			.user(minimalUserData)
			.build()
	}
}
