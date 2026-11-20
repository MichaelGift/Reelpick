package com.myth.core.recommend

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A user's taste as a weight per feature (a genre id for films, a genre or tag for games), built
 * from explicit signals only. Weights are normalised so the strongest feature has magnitude 1;
 * negative weights mean "avoid".
 */
class TasteProfile<F : Comparable<F>> private constructor(val weights: Map<F, Double>) {

    private val norm: Double = sqrt(weights.values.sumOf { it * it })

    /** True when nothing positive is known yet, e.g. before onboarding. */
    val isEmpty: Boolean get() = weights.values.none { it > 0.0 }

    fun weightOf(feature: F): Double = weights[feature] ?: 0.0

    /** Positively weighted features, strongest first; ties break by natural order so output is stable. */
    fun top(limit: Int, where: (F) -> Boolean = { true }): List<F> = weights.entries
        .filter { it.value > 0.0 && where(it.key) }
        .sortedWith(compareByDescending<Map.Entry<F, Double>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { it.key }

    /**
     * Cosine similarity between this profile and an item's (one-hot) feature vector, in [-1, 1].
     * Returns 0 when either side carries no information.
     */
    fun affinity(features: Collection<F>): Double {
        val distinct = features.distinct()
        if (distinct.isEmpty() || norm == 0.0) return 0.0
        val dot = distinct.sumOf { weightOf(it) }
        return (dot / (norm * sqrt(distinct.size.toDouble()))).coerceIn(-1.0, 1.0)
    }

    /**
     * Affinity as a 0–100 "taste match" for display. Null when the profile is empty or the item
     * shares nothing the user has shown interest in, so the UI never shows a meaningless 0%.
     */
    fun matchPercent(features: Collection<F>): Int? {
        if (isEmpty) return null
        val affinity = affinity(features)
        if (affinity <= 0.0) return null
        return (affinity * 100).roundToInt().coerceIn(1, 99)
    }

    /** The item's feature this user cares about most; used to keep a feed varied. */
    fun dominant(features: Collection<F>): F? = features.maxByOrNull { weightOf(it) }

    /** Accumulates weighted signals, e.g. `add(likedGame.genres, +2.0)`. */
    class Builder<F : Comparable<F>> {
        private val raw = mutableMapOf<F, Double>()

        fun add(features: Collection<F>, delta: Double): Builder<F> = apply {
            if (delta != 0.0) features.distinct().forEach { raw[it] = (raw[it] ?: 0.0) + delta }
        }

        fun build(): TasteProfile<F> {
            val max = raw.values.maxOfOrNull { abs(it) } ?: 0.0
            if (max == 0.0) return empty()
            return TasteProfile(raw.filterValues { it != 0.0 }.mapValues { it.value / max })
        }
    }

    companion object {
        fun <F : Comparable<F>> empty(): TasteProfile<F> = TasteProfile(emptyMap())
    }
}
