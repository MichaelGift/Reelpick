package com.myth.playpick.domain.recommend

import com.myth.core.recommend.Ranked
import com.myth.core.recommend.Ranker
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.repository.FeedCacheRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the home feed: profile → candidates → ranking → rows, then caches the result.
 *
 * One candidate pool, fetched for all of the user's platforms, serves every platform scope, so
 * switching between them is instant and offline. The pool is reused while the seeds and top
 * features are unchanged, so a dislike re-ranks instantly; a new favourite or `forceRefresh`
 * fetches fresh candidates.
 */
@Singleton
class GetForYouFeedUseCase @Inject constructor(
    private val tasteProfile: GetTasteProfileUseCase,
    private val preferences: UserPreferencesRepository,
    private val candidateSource: CandidateSource,
    private val feedCache: FeedCacheRepository,
    private val json: Json,
    private val clock: Clock,
) {
    private data class PoolKey(val seeds: List<Int>, val features: List<GameFeature>, val platforms: Set<Platform>)

    private val ranker = Ranker(GameTraits, tieBreak = compareBy<Game> { it.id })
    private val lock = Mutex()
    private var pool: CandidatePool? = null
    private var poolKey: PoolKey? = null

    suspend fun cachedFeed(scope: Platform?): GameFeed? =
        feedCache.cached(scopeKey(scope))?.let { runCatching { json.decodeFromString(GameFeed.serializer(), it) }.getOrNull() }

    suspend operator fun invoke(scope: Platform? = null, forceRefresh: Boolean = false): Result<GameFeed> = lock.withLock {
        val snapshot = tasteProfile.snapshot()
        val profile = snapshot.profile
        val platforms = preferences.preferences.first().platforms
        val seeds = snapshot.seeds.take(MAX_SEEDS)
        val today = LocalDate.now(clock)

        val key = PoolKey(seeds.map { it.id }, profile.top(MAX_FEATURES), platforms)
        val candidates = pool?.takeIf { !forceRefresh && key == poolKey }
            ?: candidateSource.fetch(seeds, profile, platforms, today).getOrElse { return Result.failure(it) }
                .also { pool = it; poolKey = key }

        val names = nameIndex(candidates, seeds)
        val inScope: (Game) -> Boolean = { game ->
            scope == null || game.platforms.isEmpty() || game.platforms.any { it.id in scope.rawgIds }
        }
        val excluded = snapshot.excludedIds + seeds.map { it.id }
        val ranked = ranker.rank(
            candidates = candidates.candidates.filter { inScope(it.item) },
            profile = profile,
            excluded = excluded,
            today = today,
            // Tags like "Atmospheric" are on everything; variety comes from genres.
            diversityFeature = { it.kind == GameFeature.Kind.GENRE },
        ).map { it.toPick(profile, seeds, names) }

        val feed = GameFeed(
            topPicks = ranked.take(TOP_PICKS),
            becauseYouLiked = seeds.take(SEED_ROWS).map { seed ->
                SeedRow(seed, ranked.filter { pick -> seed.id in candidates.seedsOf(pick.game.id) }.take(ROW_SIZE))
            }.filter { it.picks.isNotEmpty() },
            genreRows = profile.top(GENRE_ROWS) { it.kind == GameFeature.Kind.GENRE }.mapNotNull { feature ->
                val name = names[feature] ?: return@mapNotNull null
                GenreRow(Named(feature.id, name), ranked.filter { pick -> pick.game.genres.any { it.id == feature.id } }.take(ROW_SIZE))
            }.filter { it.picks.isNotEmpty() },
            shortAndSweet = ranked.filter { it.game.playtimeHours in 1..SHORT_HOURS && (it.match ?: 0) >= SHORT_MIN_MATCH }.take(ROW_SIZE),
            trending = candidates.trending.filter { inScope(it) && it.id !in excluded }.take(ROW_SIZE),
            taste = TasteSummary(
                genres = profile.top(TASTE_FEATURES) { it.kind == GameFeature.Kind.GENRE }.mapNotNull { f -> names[f]?.let { Weighted(it, profile.weightOf(f)) } },
                tags = profile.top(TASTE_FEATURES) { it.kind == GameFeature.Kind.TAG }.mapNotNull { f -> names[f]?.let { Weighted(it, profile.weightOf(f)) } },
                liked = snapshot.feedback.count { it.type == FeedbackType.LIKE },
                finished = snapshot.library.count { it.status == LibraryStatus.COMPLETED || it.status == LibraryStatus.MASTERED },
            ),
            generatedAtMillis = clock.millis(),
        )
        feedCache.save(scopeKey(scope), json.encodeToString(GameFeed.serializer(), feed))
        Result.success(feed)
    }

    /** Drops the in-memory pool, e.g. after a taste reset or a platform change. */
    suspend fun invalidate() = lock.withLock {
        pool = null
        poolKey = null
    }

    private fun CandidatePool.seedsOf(gameId: Int): Set<Int> = candidates.firstOrNull { it.item.id == gameId }?.seedKeys.orEmpty()

    /** Genre and tag names come with the games themselves, so no extra lookups are needed. */
    private fun nameIndex(pool: CandidatePool, seeds: List<Game>): Map<GameFeature, String> = buildMap {
        (pool.candidates.map { it.item } + seeds).forEach { game ->
            game.genres.forEach { put(GameFeature.genre(it.id), it.name) }
            game.tags.forEach { put(GameFeature.tag(it.id), it.name) }
        }
    }

    private fun Ranked<Game, Int>.toPick(profile: GameTasteProfile, seeds: List<Game>, names: Map<GameFeature, String>): Pick {
        val reason = seeds.firstOrNull { it.id in candidate.seedKeys }?.let { Reason.BecauseYouLiked(it.id, it.name) }
            ?: item.features
                .filter { profile.weightOf(it) > 0.0 }
                .sortedByDescending { profile.weightOf(it) }
                .mapNotNull { names[it] }
                .take(2)
                .takeIf { it.isNotEmpty() }
                ?.let { Reason.Matches(it) }
            ?: Reason.Trending
        return Pick(item, score, reason, match)
    }

    companion object {
        const val MAX_SEEDS = 5
        const val MAX_FEATURES = 6
        const val TOP_PICKS = 10
        const val SEED_ROWS = 3
        const val GENRE_ROWS = 2
        const val ROW_SIZE = 15
        const val TASTE_FEATURES = 5
        const val SHORT_HOURS = 10
        const val SHORT_MIN_MATCH = 60

        fun scopeKey(scope: Platform?) = scope?.name ?: "ALL"
    }
}
