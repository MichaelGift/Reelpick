package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.MovieRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the home feed: profile → candidates → ranking → sections, then caches the result.
 *
 * The candidate pool is reused while the user's seeds and top genres are unchanged, so a dislike or
 * "not interested" re-ranks instantly and offline; a new like or [invoke] with `forceRefresh`
 * fetches fresh candidates.
 */
@Singleton
class GetForYouFeedUseCase @Inject constructor(
    private val movies: MovieRepository,
    private val feedbackRepository: FeedbackRepository,
    private val watchlistRepository: WatchlistRepository,
    private val preferences: UserPreferencesRepository,
    private val feedCache: FeedCacheRepository,
    private val candidateSource: CandidateSource,
    private val ranker: RecommendationRanker,
    private val clock: Clock,
) {
    private data class PoolKey(val seedIds: List<Int>, val genreIds: List<Int>)

    private val lock = Mutex()
    private var pool: CandidatePool? = null
    private var poolKey: PoolKey? = null

    val cachedFeed: Flow<ForYouFeed?> get() = feedCache.cachedFeed

    suspend operator fun invoke(forceRefresh: Boolean = false): Result<ForYouFeed> = lock.withLock {
        val prefs = preferences.preferences.first()
        val feedback = feedbackRepository.feedback.first()
        val watchlist = watchlistRepository.watchlist.first()

        val profile = TasteProfile.build(prefs.selectedGenreIds, feedback, watchlist)
        val seeds = feedback
            .filter { it.type == FeedbackType.LIKE }
            .sortedByDescending { it.timestampMillis }
            .map { it.movie }
            .take(MAX_SEEDS)
        val topGenres = profile.topGenres(MAX_GENRES)

        val key = PoolKey(seeds.map { it.id }, topGenres)
        val candidates = pool?.takeIf { !forceRefresh && key == poolKey }
            ?: candidateSource.fetch(seeds, topGenres).getOrElse { return Result.failure(it) }
                .also { pool = it; poolKey = key }

        val genreNames = movies.genres().getOrDefault(emptyList()).associate { it.id to it.name }
        val excluded = feedback.mapTo(mutableSetOf()) { it.movie.id } + watchlist.map { it.id }
        val ranked = ranker.rank(
            candidates = candidates.candidates,
            profile = profile,
            seeds = seeds,
            excludedIds = excluded,
            genreNames = genreNames,
            today = LocalDate.now(clock),
        )

        val seedsByCandidate = candidates.candidates.associate { it.movie.id to it.seedIds }
        val feed = ForYouFeed(
            topPicks = ranked.take(TOP_PICKS),
            becauseYouLiked = seeds.take(SEED_ROWS).map { seed ->
                SeedRow(seed, ranked.filter { seed.id in seedsByCandidate[it.movie.id].orEmpty() }.take(ROW_SIZE))
            }.filter { it.items.isNotEmpty() },
            genreRows = topGenres.take(GENRE_ROWS).mapNotNull { genreId ->
                val name = genreNames[genreId] ?: return@mapNotNull null
                GenreRow(Genre(genreId, name), ranked.filter { genreId in it.movie.genreIds }.take(ROW_SIZE))
            }.filter { it.items.isNotEmpty() },
            trending = candidates.trending.filterNot { it.id in excluded }.take(ROW_SIZE),
            generatedAtMillis = clock.millis(),
            genreNames = genreNames,
            taste = TasteSummary(
                topGenres = profile.topGenres(TASTE_GENRES).mapNotNull { id ->
                    genreNames[id]?.let { GenreWeight(it, profile.weightOf(id)) }
                },
                likes = feedback.count { it.type == FeedbackType.LIKE },
                dislikes = feedback.count { it.type == FeedbackType.DISLIKE || it.type == FeedbackType.NOT_INTERESTED },
                saved = watchlist.size,
            ),
        )
        feedCache.save(feed)
        Result.success(feed)
    }

    /** Drops the in-memory pool, e.g. after the user resets their taste profile. */
    suspend fun invalidate() = lock.withLock {
        pool = null
        poolKey = null
    }

    companion object {
        const val MAX_SEEDS = 5
        const val MAX_GENRES = 3
        const val TOP_PICKS = 12
        const val SEED_ROWS = 3
        const val GENRE_ROWS = 2
        const val ROW_SIZE = 15
        const val TASTE_GENRES = 5
    }
}
