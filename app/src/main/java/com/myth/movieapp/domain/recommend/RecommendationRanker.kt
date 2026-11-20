package com.myth.movieapp.domain.recommend

import com.myth.core.recommend.ItemTraits
import com.myth.core.recommend.Ranker
import com.myth.core.recommend.RankerWeights
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import java.time.LocalDate
import javax.inject.Inject
import com.myth.core.recommend.Candidate as RankCandidate

/** How the shared engine reads a film or series. */
object TitleTraits : ItemTraits<Title, TitleKey, Int> {
    override fun key(item: Title) = item.key
    override fun features(item: Title) = item.genreIds
    override fun rating(item: Title) = item.voteAverage
    override fun votes(item: Title) = item.voteCount
    override fun released(item: Title) = item.releaseLocalDate()
}

/**
 * Reelpick's use of the shared [Ranker]: ranks films and series, drops the seeds themselves and
 * anything already rated, saved or watched, and explains each pick.
 */
class RecommendationRanker(weights: RankerWeights) {

    @Inject constructor() : this(RankerWeights())

    private val engine = Ranker(TitleTraits, weights, compareBy<Title> { it.id }.thenBy { it.mediaType })

    fun rank(
        candidates: List<Candidate>,
        profile: TasteProfile,
        seeds: List<Title>,
        excludedKeys: Set<TitleKey>,
        genreNames: Map<Int, String>,
        today: LocalDate,
    ): List<Recommendation> {
        val seedKeys = seeds.mapTo(mutableSetOf()) { it.key }
        return engine
            .rank(candidates.map { RankCandidate(it.movie, it.seedKeys) }, profile, excludedKeys + seedKeys, today)
            .map { ranked ->
                Recommendation(
                    movie = ranked.item,
                    score = ranked.score,
                    reason = reasonFor(ranked.item, ranked.candidate.seedKeys, profile, seeds, genreNames),
                    match = ranked.match,
                )
            }
    }

    internal fun bayesianRating(movie: Title): Double = engine.bayesianRating(movie)

    internal fun freshness(movie: Title, today: LocalDate): Double = engine.freshness(movie, today)

    private fun reasonFor(
        movie: Title,
        reachedFrom: Set<TitleKey>,
        profile: TasteProfile,
        seeds: List<Title>,
        genreNames: Map<Int, String>,
    ): RecommendationReason {
        // Seeds are ordered most recent like first, so the reason cites the freshest memory.
        seeds.firstOrNull { it.key in reachedFrom }?.let {
            return RecommendationReason.BecauseYouLiked(it.id, it.title)
        }
        val matched = movie.genreIds
            .filter { profile.weightOf(it) > 0.0 }
            .sortedByDescending { profile.weightOf(it) }
            .mapNotNull { genreNames[it] }
            .take(2)
        return if (matched.isNotEmpty()) RecommendationReason.MatchesGenres(matched) else RecommendationReason.Trending
    }
}
