package com.myth.playpick.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** A genre, tag, developer, publisher, platform or store: RAWG gives them all an id, name and slug. */
@Serializable
data class Named(val id: Int, val name: String, val slug: String = "")

/**
 * What the taste profile scores. Genres and tags are separate spaces because their ids overlap
 * (genre 4 is Action, tag 4 is Funny).
 */
@Serializable
data class GameFeature(val kind: Kind, val id: Int) : Comparable<GameFeature> {
    enum class Kind { GENRE, TAG }

    override fun compareTo(other: GameFeature): Int = compareValuesBy(this, other, { it.kind }, { it.id })

    companion object {
        fun genre(id: Int) = GameFeature(Kind.GENRE, id)
        fun tag(id: Int) = GameFeature(Kind.TAG, id)
    }
}

@Serializable
data class Game(
    val id: Int,
    val slug: String,
    val name: String,
    val coverUrl: String? = null,
    /** "yyyy-MM-dd", or null when unannounced. */
    val released: String? = null,
    val tba: Boolean = false,
    /** RAWG community rating, 0–5. */
    val rating: Double = 0.0,
    val ratingsCount: Int = 0,
    val metacritic: Int? = null,
    /** Average hours to play, from RAWG; 0 when unknown. */
    val playtimeHours: Int = 0,
    val genres: List<Named> = emptyList(),
    /** Taste-bearing tags only; storefront and feature tags are filtered out when mapping. */
    val tags: List<Named> = emptyList(),
    /** Parent platforms (PC, PlayStation, Xbox, Nintendo...). */
    val platforms: List<Named> = emptyList(),
    val esrb: String? = null,
    val screenshots: List<String> = emptyList(),
) {
    val features: List<GameFeature>
        get() = genres.map { GameFeature.genre(it.id) } + tags.map { GameFeature.tag(it.id) }

    val releaseYear: String? get() = released?.take(4)?.takeIf { it.length == 4 }

    fun releaseLocalDate(): LocalDate? = released?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

@Serializable
data class StoreLink(val store: Named, val url: String)

data class GameDetails(
    val game: Game,
    val description: String,
    val developers: List<Named>,
    val publishers: List<Named>,
    val website: String?,
    val stores: List<StoreLink>,
    val screenshots: List<String>,
    /** Distinct achievements (RAWG's per-platform total double counts). */
    val achievementCount: Int,
    val seriesCount: Int,
    val additionsCount: Int,
)

@Serializable
data class Achievement(
    val id: Long,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    /** Share of players who unlocked it, 0–100; null when RAWG doesn't know. */
    val percent: Double? = null,
) {
    val rarity: Rarity? get() = percent?.let(Rarity::of)
}

enum class Rarity {
    COMMON, UNCOMMON, RARE, ULTRA_RARE;

    companion object {
        fun of(percent: Double): Rarity = when {
            percent >= 50 -> COMMON
            percent >= 20 -> UNCOMMON
            percent >= 5 -> RARE
            else -> ULTRA_RARE
        }
    }
}

/** The platforms people choose in onboarding, mapped to RAWG parent platform ids. */
enum class Platform(val label: String, val rawgIds: List<Int>) {
    PC("PC", listOf(1)),
    PLAYSTATION("PlayStation", listOf(2)),
    XBOX("Xbox", listOf(3)),
    NINTENDO("Nintendo", listOf(7)),
    MOBILE("Mobile", listOf(4, 8)),
    MAC("Mac", listOf(5)),
    ;

    companion object {
        fun rawgIds(platforms: Collection<Platform>): List<Int> = platforms.flatMap { it.rawgIds }.distinct().sorted()
    }
}

data class Paged<T>(val items: List<T>, val page: Int, val hasMore: Boolean)

/** A filtered list request; null or empty filters are left out. */
data class GameQuery(
    val search: String? = null,
    val genreIds: Set<Int> = emptySet(),
    val tagIds: Set<Int> = emptySet(),
    val developerSlugs: Set<String> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val ordering: Ordering? = null,
    val metacritic: IntRange? = null,
    val released: ClosedRange<LocalDate>? = null,
    val page: Int = 1,
    val pageSize: Int = 40,
) {
    enum class Ordering(val param: String) {
        POPULAR("-added"),
        RATING("-rating"),
        METACRITIC("-metacritic"),
        NEWEST("-released"),
    }
}

enum class FeedbackType { LIKE, DISLIKE, NOT_INTERESTED }

data class Feedback(val game: Game, val type: FeedbackType, val createdAtMillis: Long)

/** Where a game sits in the user's library. Finishing, and above all 100%-ing, is the strongest taste signal. */
enum class LibraryStatus(val label: String) {
    BACKLOG("Backlog"),
    PLAYING("Playing"),
    COMPLETED("Completed"),
    MASTERED("100%"),
    DROPPED("Dropped"),
}

data class Progress(val unlocked: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else unlocked.toFloat() / total
    val isComplete: Boolean get() = total > 0 && unlocked >= total
}

data class LibraryEntry(
    val game: Game,
    val status: LibraryStatus,
    val addedAtMillis: Long,
    val updatedAtMillis: Long,
    val progress: Progress,
)

data class ChecklistItem(val achievement: Achievement, val unlockedAtMillis: Long?) {
    val unlocked: Boolean get() = unlockedAtMillis != null
}

/** A game's achievements with the user's ticks. [syncedAtMillis] is null until the list was first fetched. */
data class Checklist(val gameId: Int, val items: List<ChecklistItem>, val syncedAtMillis: Long?) {
    val progress: Progress get() = Progress(items.count { it.unlocked }, items.size)
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val platforms: Set<Platform> = emptySet(),
    val genreIds: Set<Int> = emptySet(),
    val tagIds: Set<Int> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val recentSearches: List<String> = emptyList(),
    /** For You narrowed to one platform; null means all of the user's platforms. */
    val feedPlatform: Platform? = null,
)
