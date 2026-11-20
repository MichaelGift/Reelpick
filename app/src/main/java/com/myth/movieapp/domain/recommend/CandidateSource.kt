package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.repository.TitleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/** Where a candidate came from. A title can arrive through several origins. */
sealed interface CandidateOrigin {
    data class Seed(val seed: TitleKey) : CandidateOrigin
    data class Genre(val genreId: Int) : CandidateOrigin
    data object Trending : CandidateOrigin
}

data class Candidate(val movie: Title, val origins: Set<CandidateOrigin>) {
    val seedKeys: Set<TitleKey> get() = origins.filterIsInstance<CandidateOrigin.Seed>().mapTo(mutableSetOf()) { it.seed }
}

data class CandidatePool(val candidates: List<Candidate>, val trending: List<Title>)

/**
 * Gathers films and series worth ranking: TMDB recommendations for each liked "seed" (a liked
 * series yields series, a liked film yields films), well-voted titles of both types in the user's
 * top genres, and this week's trending titles for exploration.
 * Sources are fetched concurrently; the pool fails only if every source fails.
 */
class CandidateSource @Inject constructor(private val titles: TitleRepository) {

    suspend fun fetch(seeds: List<Title>, genreIds: List<Int>): Result<CandidatePool> = coroutineScope {
        val seedCalls = seeds.map { seed ->
            async { CandidateOrigin.Seed(seed.key) to titles.recommendationsFor(seed.key) }
        }
        val genreCalls = genreIds.flatMap { genreId ->
            MediaType.entries.map { type ->
                async { CandidateOrigin.Genre(genreId) to titles.discoverByGenre(type, genreId) }
            }
        }
        val trendingCalls = MediaType.entries.map { type -> async { titles.trending(type) } }

        val sourced = (seedCalls + genreCalls).awaitAll()
        val trendingResults = trendingCalls.awaitAll()
        val all = sourced.map { it.second } + trendingResults
        if (all.all { it.isFailure }) {
            return@coroutineScope Result.failure(all.first().exceptionOrNull()!!)
        }

        // Films and series alternate so neither type crowds the other out of the trending row.
        val trending = interleave(trendingResults.map { it.getOrDefault(emptyList()) })

        val merged = LinkedHashMap<TitleKey, Candidate>()
        val withOrigins = sourced + trending.map { CandidateOrigin.Trending to Result.success(listOf(it)) }
        for ((origin, result) in withOrigins) {
            for (title in result.getOrDefault(emptyList())) {
                val existing = merged[title.key]
                merged[title.key] = existing?.copy(origins = existing.origins + origin) ?: Candidate(title, setOf(origin))
            }
        }
        Result.success(CandidatePool(merged.values.toList(), trending))
    }

    private fun <T> interleave(lists: List<List<T>>): List<T> =
        (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }
}
