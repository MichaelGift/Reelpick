package com.myth.movieapp.data.repository

import com.myth.movieapp.data.local.GenreDao
import com.myth.movieapp.data.local.GenreEntity
import com.myth.movieapp.data.remote.TmdbApi
import com.myth.movieapp.data.remote.toDomain
import com.myth.movieapp.data.remote.toPagedMovies
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.PagedMovies
import com.myth.movieapp.domain.repository.MovieRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val genreDao: GenreDao,
) : MovieRepository {

    private val genreLock = Mutex()
    @Volatile private var genreCache: List<Genre>? = null

    /** Genres rarely change: memory first, then network, falling back to the Room copy offline. */
    override suspend fun genres(): Result<List<Genre>> = genreLock.withLock {
        genreCache?.let { return Result.success(it) }
        val remote = safeCall { api.genres().genres.map { it.toDomain() } }
        remote.onSuccess { genres ->
            genreDao.replaceAll(genres.map { GenreEntity(it.id, it.name) })
            genreCache = genres
        }
        if (remote.isSuccess) return remote

        val cached = genreDao.getAll().map { Genre(it.id, it.name) }
        if (cached.isNotEmpty()) Result.success(cached.also { genreCache = it }) else remote
    }

    override suspend fun popular(page: Int): Result<PagedMovies> =
        safeCall { api.popular(page).toPagedMovies() }

    override suspend fun topRated(page: Int): Result<PagedMovies> =
        safeCall { api.topRated(page).toPagedMovies() }

    override suspend fun trending(): Result<List<Movie>> =
        safeCall { api.trending().results.map { it.toDomain() } }

    override suspend fun recommendationsFor(movieId: Int): Result<List<Movie>> =
        safeCall { api.recommendations(movieId).results.map { it.toDomain() } }

    override suspend fun discoverByGenre(genreId: Int, page: Int): Result<List<Movie>> =
        safeCall { api.discover(genreIds = genreId.toString(), page = page).results.map { it.toDomain() } }

    override suspend fun search(query: String, page: Int): Result<PagedMovies> =
        safeCall { api.search(query, page).toPagedMovies() }

    override suspend fun details(movieId: Int, region: String): Result<MovieDetails> =
        safeCall { api.details(movieId).toDomain(region) }
}
