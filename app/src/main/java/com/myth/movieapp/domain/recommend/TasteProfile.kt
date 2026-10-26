package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A user's taste as a weight per TMDB genre id, built from explicit signals only.
 * Weights are normalised so the strongest genre has magnitude 1; negative weights mean "avoid".
 */
class TasteProfile private constructor(val genreWeights: Map<Int, Double>) {

    private val norm: Double = sqrt(genreWeights.values.sumOf { it * it })

    /** True when nothing positive is known yet, e.g. before onboarding. */
    val isEmpty: Boolean get() = genreWeights.values.none { it > 0.0 }

    fun weightOf(genreId: Int): Double = genreWeights[genreId] ?: 0.0

    /** Positively weighted genres, strongest first. */
    fun topGenres(limit: Int): List<Int> = genreWeights.entries
        .filter { it.value > 0.0 }
        .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { it.key }

    /**
     * Cosine similarity between this profile and a movie's (one-hot) genre vector, in [-1, 1].
     * Returns 0 when either side carries no information.
     */
    fun affinity(genreIds: List<Int>): Double {
        val genres = genreIds.distinct()
        if (genres.isEmpty() || norm == 0.0) return 0.0
        val dot = genres.sumOf { weightOf(it) }
        return (dot / (norm * sqrt(genres.size.toDouble()))).coerceIn(-1.0, 1.0)
    }

    /**
     * Affinity as a 0–100 "taste match" for display. Null when the profile is empty or the movie
     * shares nothing the user has shown interest in, so the UI never shows a meaningless 0%.
     */
    fun matchPercent(genreIds: List<Int>): Int? {
        if (isEmpty) return null
        val affinity = affinity(genreIds)
        if (affinity <= 0.0) return null
        return (affinity * 100).roundToInt().coerceIn(1, 99)
    }

    /** The movie's genre this user cares about most; used to keep the feed varied. */
    fun dominantGenre(genreIds: List<Int>): Int? = genreIds.maxByOrNull { weightOf(it) }

    companion object {
        const val SELECTED_GENRE = 1.0
        const val LIKE = 2.0
        const val DISLIKE = -2.0
        const val NOT_INTERESTED = -2.0
        const val WATCHLIST = 1.0

        val EMPTY = TasteProfile(emptyMap())

        fun build(
            selectedGenreIds: Set<Int>,
            feedback: List<Feedback>,
            watchlist: List<Movie>,
        ): TasteProfile {
            val raw = mutableMapOf<Int, Double>()
            fun add(genreIds: Collection<Int>, delta: Double) =
                genreIds.distinct().forEach { raw[it] = (raw[it] ?: 0.0) + delta }

            add(selectedGenreIds, SELECTED_GENRE)
            feedback.forEach { signal ->
                val delta = when (signal.type) {
                    FeedbackType.LIKE -> LIKE
                    FeedbackType.DISLIKE -> DISLIKE
                    FeedbackType.NOT_INTERESTED -> NOT_INTERESTED
                    FeedbackType.WATCHED -> 0.0 // Watching says nothing about whether they enjoyed it.
                }
                if (delta != 0.0) add(signal.movie.genreIds, delta)
            }
            watchlist.forEach { add(it.genreIds, WATCHLIST) }

            val max = raw.values.maxOfOrNull { abs(it) } ?: 0.0
            if (max == 0.0) return EMPTY
            return TasteProfile(raw.filterValues { it != 0.0 }.mapValues { it.value / max })
        }
    }
}
