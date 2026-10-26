package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Why a movie was recommended; shown to the user as a short label. */
@Serializable
sealed interface RecommendationReason {
    @Serializable
    @SerialName("liked")
    data class BecauseYouLiked(val seedId: Int, val seedTitle: String) : RecommendationReason

    @Serializable
    @SerialName("genres")
    data class MatchesGenres(val genreNames: List<String>) : RecommendationReason

    @Serializable
    @SerialName("trending")
    data object Trending : RecommendationReason
}

@Serializable
data class Recommendation(
    val movie: Movie,
    val score: Double,
    val reason: RecommendationReason,
    /** Taste match 0–100, or null when there's no meaningful overlap with the user's profile. */
    val match: Int? = null,
)

@Serializable
data class GenreWeight(val name: String, val weight: Double)

/** What the app has learned, shown back to the user so the feed never feels like a black box. */
@Serializable
data class TasteSummary(
    val topGenres: List<GenreWeight>,
    val likes: Int,
    val dislikes: Int,
    val saved: Int,
)

@Serializable
data class SeedRow(val seed: Movie, val items: List<Recommendation>)

@Serializable
data class GenreRow(val genre: Genre, val items: List<Recommendation>)

@Serializable
data class ForYouFeed(
    val topPicks: List<Recommendation>,
    val becauseYouLiked: List<SeedRow>,
    val genreRows: List<GenreRow>,
    val trending: List<Movie>,
    val generatedAtMillis: Long,
    val genreNames: Map<Int, String> = emptyMap(),
    val taste: TasteSummary? = null,
) {
    val isEmpty: Boolean get() = topPicks.isEmpty() && trending.isEmpty()
}
