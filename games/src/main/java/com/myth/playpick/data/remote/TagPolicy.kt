package com.myth.playpick.data.remote

import com.myth.playpick.data.remote.dto.TagDto

/**
 * RAWG tags mix taste ("Metroidvania", "Souls-like", "Story Rich") with storefront features
 * ("Steam Achievements", "Full controller support") and come in English and Russian. Only the first
 * kind says anything about what someone enjoys, so the rest never reach the taste profile.
 */
object TagPolicy {
    /** Enough to describe a game; RAWG lists the most-used tags first. */
    const val MAX_TAGS = 10

    private val blockedIds = setOf(
        31, // Singleplayer: nearly every game, so it carries no signal
        7, // Multiplayer: same
        42, // Great Soundtrack
        411, // "cooperative": duplicates 18 "Co-op"
        40847, // Steam Achievements
        40836, // Full controller support
        40849, // Steam Cloud
        7808, // Steam Trading Cards
        40845, // Partial Controller Support
        40850, // Steam Leaderboards
    )

    private val blockedSlugParts = listOf("steam", "controller", "achievements", "cloud", "trading-cards", "workshop", "captions")

    fun isTasteTag(tag: TagDto): Boolean =
        (tag.language == null || tag.language == "eng") &&
            tag.id !in blockedIds &&
            blockedSlugParts.none { it in tag.slug }

    fun filter(tags: List<TagDto>): List<TagDto> = tags.filter(::isTasteTag).take(MAX_TAGS)
}
