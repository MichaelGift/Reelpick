package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.FeedScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.TitleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the home feed: profile → candidates → ranking → sections, then caches the result.
 *
 * One candidate pool of films and series serves every [FeedScope]: switching between All, Movies
 * and Series only filters and re-ranks it, so it's instant and works offline. The pool is reused
 * while the user's seeds and top genres are unchanged, so a dislike or "not interested" re-ranks
 * instantly too; a new like or [invoke] with `forceRefresh` fetches fresh candidates.
 */
@Singleton
class GetForYouFeedUseCase @Inject constructor(
    private val titles: TitleRepository,
    private val tasteProfile: GetTasteProfileUseCase,
    private val feedCache: FeedCacheRepository,
    private val candidateSource: CandidateSource,
    private val ranker: RecommendationRanker,
    private val clock: Clock,
) {
    private data class PoolKey(val seeds: List<TitleKey>, val genreIds: List<Int>)

    private val lock = Mutex()
    private var pool: CandidatePool? = null
    private var poolKey: PoolKey? = null

    fun cachedFeed(scope: FeedScope): Flow<ForYouFeed?> = feedCache.cachedFeed(scope)

    suspend operator fun invoke(scope: FeedScope = FeedScope.ALL, forceRefresh: Boolean = false): Result<ForYouFeed> =
        lock.withLock {
            val (profile, feedback, watchlist) = tasteProfile.snapshot()
            // Seeds of every type feed the shared pool; rows are filtered to the scope below.
            val seeds = feedback
                .filter { it.type == FeedbackType.LIKE }
                .sortedByDescending { it.timestampMillis }
                .map { it.movie }
                .take(MAX_SEEDS)
            val topGenres = profile.top(MAX_GENRES)

            val key = PoolKey(seeds.map { it.key }, topGenres)
            val candidates = pool?.takeIf { !forceRefresh && key == poolKey }
                ?: candidateSource.fetch(seeds, topGenres).getOrElse { return Result.failure(it) }
                    .also { pool = it; poolKey = key }

            val genreNames = titles.genres().getOrDefault(emptyList()).associate { it.id to it.name }
            val excluded = feedback.mapTo(mutableSetOf()) { it.movie.key } + watchlist.map { it.key }
            val ranked = ranker.rank(
                candidates = candidates.candidates.filter { scope.includes(it.movie.mediaType) },
                profile = profile,
                seeds = seeds,
                excludedKeys = excluded,
                genreNames = genreNames,
                today = LocalDate.now(clock),
            )

            val seedsByCandidate = candidates.candidates.associate { it.movie.key to it.seedKeys }
            val feed = ForYouFeed(
                topPicks = ranked.take(TOP_PICKS),
                becauseYouLiked = seeds.take(SEED_ROWS).map { seed ->
                    SeedRow(seed, ranked.filter { seed.key in seedsByCandidate[it.movie.key].orEmpty() }.take(ROW_SIZE))
                }.filter { it.items.isNotEmpty() },
                genreRows = topGenres.take(GENRE_ROWS).mapNotNull { genreId ->
                    val name = genreNames[genreId] ?: return@mapNotNull null
                    GenreRow(Genre(genreId, name), ranked.filter { genreId in it.movie.genreIds }.take(ROW_SIZE))
                }.filter { it.items.isNotEmpty() },
                trending = candidates.trending
                    .filter { scope.includes(it.mediaType) && it.key !in excluded }
                    .take(ROW_SIZE),
                generatedAtMillis = clock.millis(),
                genreNames = genreNames,
                taste = TasteSummary(
                    topGenres = profile.top(TASTE_GENRES).mapNotNull { id ->
                        genreNames[id]?.let { GenreWeight(it, profile.weightOf(id)) }
                    },
                    likes = feedback.count { it.type == FeedbackType.LIKE },
                    dislikes = feedback.count { it.type == FeedbackType.DISLIKE || it.type == FeedbackType.NOT_INTERESTED },
                    saved = watchlist.size,
                ),
            )
            feedCache.save(scope, feed)
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
