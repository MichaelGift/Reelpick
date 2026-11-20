package com.myth.core.recommend

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How the engine reads an app's item type, so the same ranker works for films and games. */
interface ItemTraits<T, K, F> {
    fun key(item: T): K
    fun features(item: T): Collection<F>

    /** Average rating on a 0–10 scale. */
    fun rating(item: T): Double
    fun votes(item: T): Int
    fun released(item: T): LocalDate?
}

/** An item worth ranking, and which of the user's liked "seeds" led to it. */
data class Candidate<T, K>(val item: T, val seedKeys: Set<K> = emptySet())

data class Ranked<T, K>(
    val candidate: Candidate<T, K>,
    val score: Double,
    /** Taste match 0–100, or null when there's no meaningful overlap. */
    val match: Int?,
) {
    val item: T get() = candidate.item
}

data class RankerWeights(
    val affinity: Double = 0.45,
    val seed: Double = 0.30,
    val quality: Double = 0.20,
    val freshness: Double = 0.05,
    /** Seed support saturates here: being reached from 3 liked items is "fully supported". */
    val seedSaturation: Int = 3,
    /** Bayesian prior: an item needs about this many votes before its own average dominates. */
    val priorVotes: Int = 300,
    /** Prior mean on the 0–10 scale. */
    val priorMean: Double = 6.5,
    /** Releases older than this many years get no freshness boost. */
    val freshnessYears: Double = 10.0,
    /** At most this many consecutive picks may share a dominant feature. */
    val maxRun: Int = 2,
)

/**
 * Scores and orders candidates for one user. Pure Kotlin with no I/O, so every rule is unit-tested.
 *
 * score = affinity * tasteAffinity + seed * seedSupport + quality * bayesianRating + freshness * recency
 *
 * Each term is in [0, 1]. Excluded items (already rated, saved, played) and the seeds themselves are
 * removed, and a final pass stops more than [RankerWeights.maxRun] consecutive picks sharing the
 * same dominant feature.
 */
class Ranker<T, K, F : Comparable<F>>(
    private val traits: ItemTraits<T, K, F>,
    private val weights: RankerWeights = RankerWeights(),
    /** Breaks score ties deterministically. */
    private val tieBreak: Comparator<T>,
) {

    fun rank(
        candidates: List<Candidate<T, K>>,
        profile: TasteProfile<F>,
        excluded: Set<K>,
        today: LocalDate,
        /** Narrows the features that drive variety, e.g. genres only. */
        diversityFeature: (F) -> Boolean = { true },
    ): List<Ranked<T, K>> {
        val scored = candidates
            .asSequence()
            .filter { traits.key(it.item) !in excluded }
            .distinctBy { traits.key(it.item) }
            .map { Ranked(it, score(it, profile, today), profile.matchPercent(traits.features(it.item))) }
            .sortedWith(compareByDescending<Ranked<T, K>> { it.score }.thenBy(tieBreak) { it.item })
            .toList()
        return diversify(scored) { profile.dominant(traits.features(it.item).filter(diversityFeature)) }
    }

    fun score(candidate: Candidate<T, K>, profile: TasteProfile<F>, today: LocalDate): Double {
        val item = candidate.item
        val affinity = if (profile.isEmpty) 0.5 else (profile.affinity(traits.features(item)) + 1.0) / 2.0
        val seed = candidate.seedKeys.size.coerceAtMost(weights.seedSaturation).toDouble() / weights.seedSaturation
        return weights.affinity * affinity +
            weights.seed * seed +
            weights.quality * bayesianRating(item) / 10.0 +
            weights.freshness * freshness(item, today)
    }

    /** Pulls averages from few votes toward [RankerWeights.priorMean], so 9.0 from 12 votes doesn't win. */
    fun bayesianRating(item: T): Double {
        val v = traits.votes(item).coerceAtLeast(0).toDouble()
        val m = weights.priorVotes.toDouble()
        return (v / (v + m)) * traits.rating(item) + (m / (v + m)) * weights.priorMean
    }

    fun freshness(item: T, today: LocalDate): Double {
        val released = traits.released(item) ?: return 0.0
        if (!released.isBefore(today)) return 1.0
        val years = ChronoUnit.DAYS.between(released, today) / 365.25
        return (1.0 - years / weights.freshnessYears).coerceIn(0.0, 1.0)
    }

    /**
     * Greedy re-ordering: take the best remaining item whose dominant feature wouldn't extend a run
     * longer than [RankerWeights.maxRun]; if every remaining item would, take the best one anyway.
     */
    fun <R> diversify(ranked: List<R>, dominant: (R) -> F?): List<R> {
        val maxRun = weights.maxRun
        if (maxRun <= 0 || ranked.size < 3) return ranked
        val remaining = ranked.toMutableList()
        val result = ArrayList<R>(ranked.size)
        while (remaining.isNotEmpty()) {
            val recent = result.takeLast(maxRun).map(dominant)
            val run = recent.firstOrNull()?.takeIf { f -> recent.size == maxRun && recent.all { it == f } }
            val index = remaining.indexOfFirst { run == null || dominant(it) != run }
            result += remaining.removeAt(if (index >= 0) index else 0)
        }
        return result
    }
}
