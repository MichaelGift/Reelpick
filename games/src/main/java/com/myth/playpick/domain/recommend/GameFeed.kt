package com.myth.playpick.domain.recommend

import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.Named
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Why a game was recommended; shown as a short label. */
@Serializable
sealed interface Reason {
    @Serializable
    @SerialName("liked")
    data class BecauseYouLiked(val seedId: Int, val seedName: String) : Reason

    @Serializable
    @SerialName("matches")
    data class Matches(val names: List<String>) : Reason

    @Serializable
    @SerialName("trending")
    data object Trending : Reason

    fun label(): String = when (this) {
        is BecauseYouLiked -> "Because you liked $seedName"
        is Matches -> "For ${names.joinToString(" & ")} fans"
        Trending -> "Popular right now"
    }
}

@Serializable
data class Pick(
    val game: Game,
    val score: Double,
    val reason: Reason,
    /** Taste match 0–100, or null when there's no meaningful overlap. */
    val match: Int? = null,
)

@Serializable
data class Weighted(val name: String, val weight: Double)

/** What the app has learned, shown back to the user. */
@Serializable
data class TasteSummary(
    val genres: List<Weighted>,
    val tags: List<Weighted>,
    val liked: Int,
    val finished: Int,
)

@Serializable
data class SeedRow(val seed: Game, val picks: List<Pick>)

@Serializable
data class GenreRow(val genre: Named, val picks: List<Pick>)

@Serializable
data class GameFeed(
    val topPicks: List<Pick>,
    val becauseYouLiked: List<SeedRow>,
    val genreRows: List<GenreRow>,
    /** Strong matches that take under ten hours. */
    val shortAndSweet: List<Pick>,
    val trending: List<Game>,
    val taste: TasteSummary?,
    val generatedAtMillis: Long,
) {
    val isEmpty: Boolean get() = topPicks.isEmpty() && trending.isEmpty()
}
