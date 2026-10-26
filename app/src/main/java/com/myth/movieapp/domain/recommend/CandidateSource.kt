package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/** Where a candidate came from. A movie can arrive through several origins. */
sealed interface CandidateOrigin {
    data class Seed(val seedId: Int) : CandidateOrigin
    data class Genre(val genreId: Int) : CandidateOrigin
    data object Trending : CandidateOrigin
}

data class Candidate(val movie: Movie, val origins: Set<CandidateOrigin>) {
    val seedIds: Set<Int> get() = origins.filterIsInstance<CandidateOrigin.Seed>().mapTo(mutableSetOf()) { it.seedId }
}

data class CandidatePool(val candidates: List<Candidate>, val trending: List<Movie>)

/**
 * Gathers movies worth ranking: TMDB recommendations for each liked "seed", well-voted
 * movies in the user's top genres, and this week's trending titles for exploration.
 * Sources are fetched concurrently; the pool fails only if every source fails.
 */
class CandidateSource @Inject constructor(private val movies: MovieRepository) {

    suspend fun fetch(seeds: List<Movie>, genreIds: List<Int>): Result<CandidatePool> = coroutineScope {
        val seedCalls = seeds.map { seed ->
            async { CandidateOrigin.Seed(seed.id) to movies.recommendationsFor(seed.id) }
        }
        val genreCalls = genreIds.map { genreId ->
            async { CandidateOrigin.Genre(genreId) to movies.discoverByGenre(genreId) }
        }
        val trendingCall = async { CandidateOrigin.Trending to movies.trending() }

        val results = (seedCalls + genreCalls + trendingCall).awaitAll()
        if (results.all { it.second.isFailure }) {
            return@coroutineScope Result.failure(results.first().second.exceptionOrNull()!!)
        }

        val merged = LinkedHashMap<Int, Candidate>()
        for ((origin, result) in results) {
            for (movie in result.getOrDefault(emptyList())) {
                val existing = merged[movie.id]
                merged[movie.id] = existing?.copy(origins = existing.origins + origin)
                    ?: Candidate(movie, setOf(origin))
            }
        }
        val trending = results.last().second.getOrDefault(emptyList())
        Result.success(CandidatePool(merged.values.toList(), trending))
    }
}
