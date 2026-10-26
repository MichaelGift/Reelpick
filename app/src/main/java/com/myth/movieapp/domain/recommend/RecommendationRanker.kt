package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Movie
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Scores and orders candidates for one user. Pure Kotlin with no I/O, so every rule is unit-tested.
 *
 * score = genre * genreAffinity + seed * seedSupport + quality * bayesianRating + freshness * recency
 *
 * Each term is in [0, 1]. Movies the user has already rated, saved or watched are removed, and a
 * final pass stops more than [Weights.maxGenreRun] consecutive picks sharing a dominant genre.
 */
class RecommendationRanker(private val weights: Weights) {

    @Inject constructor() : this(Weights())

    data class Weights(
        val genre: Double = 0.45,
        val seed: Double = 0.30,
        val quality: Double = 0.20,
        val freshness: Double = 0.05,
        /** Seed support saturates here: being recommended by 3 liked movies is "fully supported". */
        val seedSaturation: Int = 3,
        /** Bayesian prior: a movie needs about this many votes before its own average dominates. */
        val priorVotes: Int = 300,
        val priorMean: Double = 6.5,
        /** Releases older than this many years get no freshness boost. */
        val freshnessYears: Double = 10.0,
        val maxGenreRun: Int = 2,
    )

    fun rank(
        candidates: List<Candidate>,
        profile: TasteProfile,
        seeds: List<Movie>,
        excludedIds: Set<Int>,
        genreNames: Map<Int, String>,
        today: LocalDate,
    ): List<Recommendation> {
        val seedIds = seeds.mapTo(mutableSetOf()) { it.id }
        val scored = candidates
            .asSequence()
            .filter { it.movie.id !in excludedIds && it.movie.id !in seedIds }
            .distinctBy { it.movie.id }
            .map { candidate ->
                Recommendation(
                    movie = candidate.movie,
                    score = score(candidate, profile, today),
                    reason = reasonFor(candidate, profile, seeds, genreNames),
                    match = profile.matchPercent(candidate.movie.genreIds),
                )
            }
            .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.movie.id })
            .toList()
        return diversify(scored, profile)
    }

    internal fun score(candidate: Candidate, profile: TasteProfile, today: LocalDate): Double {
        val movie = candidate.movie
        val genre = if (profile.isEmpty) 0.5 else (profile.affinity(movie.genreIds) + 1.0) / 2.0
        val seed = candidate.seedIds.size.coerceAtMost(weights.seedSaturation).toDouble() / weights.seedSaturation
        return weights.genre * genre +
            weights.seed * seed +
            weights.quality * bayesianRating(movie) / 10.0 +
            weights.freshness * freshness(movie, today)
    }

    /** Pulls averages from few votes toward [Weights.priorMean], so 9.0 from 12 votes doesn't win. */
    internal fun bayesianRating(movie: Movie): Double {
        val v = movie.voteCount.coerceAtLeast(0).toDouble()
        val m = weights.priorVotes.toDouble()
        return (v / (v + m)) * movie.voteAverage + (m / (v + m)) * weights.priorMean
    }

    internal fun freshness(movie: Movie, today: LocalDate): Double {
        val released = movie.releaseLocalDate() ?: return 0.0
        if (!released.isBefore(today)) return 1.0
        val years = ChronoUnit.DAYS.between(released, today) / 365.25
        return (1.0 - years / weights.freshnessYears).coerceIn(0.0, 1.0)
    }

    private fun reasonFor(
        candidate: Candidate,
        profile: TasteProfile,
        seeds: List<Movie>,
        genreNames: Map<Int, String>,
    ): RecommendationReason {
        // Seeds are ordered most recent like first, so the reason cites the freshest memory.
        seeds.firstOrNull { it.id in candidate.seedIds }?.let {
            return RecommendationReason.BecauseYouLiked(it.id, it.title)
        }
        val matched = candidate.movie.genreIds
            .filter { profile.weightOf(it) > 0.0 }
            .sortedByDescending { profile.weightOf(it) }
            .mapNotNull { genreNames[it] }
            .take(2)
        return if (matched.isNotEmpty()) RecommendationReason.MatchesGenres(matched) else RecommendationReason.Trending
    }

    /**
     * Greedy re-ordering: take the best remaining item whose dominant genre wouldn't extend a run
     * longer than [Weights.maxGenreRun]; if every remaining item would, take the best one anyway.
     */
    internal fun diversify(ranked: List<Recommendation>, profile: TasteProfile): List<Recommendation> {
        if (weights.maxGenreRun <= 0 || ranked.size < 3) return ranked
        val remaining = ranked.toMutableList()
        val result = ArrayList<Recommendation>(ranked.size)
        while (remaining.isNotEmpty()) {
            val recent = result.takeLast(weights.maxGenreRun).map { profile.dominantGenre(it.movie.genreIds) }
            val runGenre = recent.firstOrNull()?.takeIf { g -> recent.size == weights.maxGenreRun && recent.all { it == g } }
            val index = remaining.indexOfFirst { runGenre == null || profile.dominantGenre(it.movie.genreIds) != runGenre }
            result += remaining.removeAt(if (index >= 0) index else 0)
        }
        return result
    }
}
