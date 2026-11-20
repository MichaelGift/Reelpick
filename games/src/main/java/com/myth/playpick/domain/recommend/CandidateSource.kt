package com.myth.playpick.domain.recommend

import com.myth.core.recommend.Candidate
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.repository.GameRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import javax.inject.Inject

data class CandidatePool(val candidates: List<Candidate<Game, Int>>, val trending: List<Game>)

/**
 * Gathers games worth ranking. RAWG's "similar games" is a paid endpoint, so a seed leads to:
 * - the rest of its series,
 * - other games by its developers,
 * - popular games sharing its top tags.
 *
 * Plus discovery in the user's strongest genres and tags, and recently popular games for
 * exploration. Every source runs concurrently and filters to the user's platforms; the pool fails
 * only if every source fails.
 */
class CandidateSource @Inject constructor(private val games: GameRepository) {

    suspend fun fetch(
        seeds: List<Game>,
        profile: GameTasteProfile,
        platforms: Set<Platform>,
        today: LocalDate,
    ): Result<CandidatePool> = coroutineScope {
        val base = GameQuery(platforms = platforms, pageSize = 20)

        val seedCalls = seeds.map { seed ->
            async {
                seed.id to coroutineScope {
                    val series = async { games.series(seed.id) }
                    val byDeveloper = async {
                        games.details(seed.id).mapCatching { details ->
                            val slugs = details.developers.take(2).map { it.slug }.filter(String::isNotBlank).toSet()
                            if (slugs.isEmpty()) emptyList() else games.games(base.copy(developerSlugs = slugs)).getOrThrow().items
                        }
                    }
                    val byTags = async {
                        val tags = seed.tags.take(SEED_TAGS).map { it.id }.toSet()
                        if (tags.isEmpty()) Result.success(emptyList()) else games.games(base.copy(tagIds = tags, ordering = GameQuery.Ordering.POPULAR)).map { it.items }
                    }
                    listOf(series.await(), byDeveloper.await(), byTags.await())
                }
            }
        }
        val topGenres = profile.top(TOP_FEATURES) { it.kind == GameFeature.Kind.GENRE }.map { it.id }.toSet()
        val topTags = profile.top(TOP_FEATURES) { it.kind == GameFeature.Kind.TAG }.map { it.id }.toSet()
        val discovery = listOfNotNull(
            topGenres.takeIf { it.isNotEmpty() }?.let { async { games.games(base.copy(genreIds = it, ordering = GameQuery.Ordering.RATING, pageSize = 40)).map { p -> p.items } } },
            topTags.takeIf { it.isNotEmpty() }?.let { async { games.games(base.copy(tagIds = it, ordering = GameQuery.Ordering.POPULAR, pageSize = 40)).map { p -> p.items } } },
        )
        val trendingCall = async {
            games.games(base.copy(ordering = GameQuery.Ordering.POPULAR, released = today.minusDays(TRENDING_DAYS)..today)).map { it.items }
        }

        val seedResults = seedCalls.awaitAll()
        val discoveryResults = discovery.awaitAll()
        val trendingResult = trendingCall.await()
        val all = seedResults.flatMap { it.second } + discoveryResults + trendingResult
        if (all.all { it.isFailure }) return@coroutineScope Result.failure(all.first().exceptionOrNull()!!)

        // Series and developer lists can't be filtered server-side, so every source is filtered here.
        // A game with no platform data is kept rather than guessed away.
        val playableIds = Platform.rawgIds(platforms).toSet()
        fun Game.playable() = playableIds.isEmpty() || this.platforms.isEmpty() || this.platforms.any { it.id in playableIds }

        val merged = LinkedHashMap<Int, Candidate<Game, Int>>()
        fun add(list: List<Game>, seedId: Int?) {
            for (game in list) {
                if (!game.playable()) continue
                val existing = merged[game.id]
                val seedKeys = (existing?.seedKeys.orEmpty()) + listOfNotNull(seedId)
                merged[game.id] = Candidate(existing?.item ?: game, seedKeys)
            }
        }
        seedResults.forEach { (seedId, results) -> results.forEach { add(it.getOrDefault(emptyList()), seedId) } }
        discoveryResults.forEach { add(it.getOrDefault(emptyList()), null) }
        val trending = trendingResult.getOrDefault(emptyList()).filter { it.playable() }
        add(trending, null)

        Result.success(CandidatePool(merged.values.toList(), trending))
    }

    private companion object {
        const val SEED_TAGS = 2
        const val TOP_FEATURES = 3
        const val TRENDING_DAYS = 90L
    }
}
