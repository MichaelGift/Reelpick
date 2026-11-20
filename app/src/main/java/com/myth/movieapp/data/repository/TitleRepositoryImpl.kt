package com.myth.movieapp.data.repository

import com.myth.movieapp.data.local.GenreDao
import com.myth.movieapp.data.local.GenreEntity
import com.myth.movieapp.data.remote.GenreMap
import com.myth.movieapp.data.remote.TmdbApi
import com.myth.movieapp.data.remote.toDomain
import com.myth.movieapp.data.remote.toPagedTitles
import com.myth.movieapp.data.remote.toSearchResults
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.PagedTitles
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.SearchResults
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.repository.TitleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val genreDao: GenreDao,
) : TitleRepository {

    private val genreLock = Mutex()
    @Volatile private var genreCache: List<Genre>? = null

    /** Genres rarely change: memory first, then network, falling back to the Room copy offline. */
    override suspend fun genres(): Result<List<Genre>> = genreLock.withLock {
        genreCache?.let { return Result.success(it) }
        val remote = safeCall {
            coroutineScope {
                val movie = async { api.genres().genres.map { it.toDomain() } }
                val tv = async { api.tvGenres().genres.map { it.toDomain() } }
                movie.await() + tv.await().filter { GenreMap.isTvOnly(it.id) }
            }
        }
        remote.onSuccess { genres ->
            genreDao.replaceAll(genres.map { GenreEntity(it.id, it.name) })
            genreCache = genres
        }
        if (remote.isSuccess) return remote

        val cached = genreDao.getAll().map { Genre(it.id, it.name) }
        if (cached.isNotEmpty()) Result.success(cached.also { genreCache = it }) else remote
    }

    override suspend fun popular(type: MediaType, page: Int): Result<PagedTitles> = safeCall {
        when (type) {
            MediaType.MOVIE -> api.popular(page).toPagedTitles()
            MediaType.TV -> api.tvPopular(page).toPagedTitles()
        }
    }

    override suspend fun topRated(type: MediaType, page: Int): Result<PagedTitles> = safeCall {
        when (type) {
            MediaType.MOVIE -> api.topRated(page).toPagedTitles()
            MediaType.TV -> api.tvTopRated(page).toPagedTitles()
        }
    }

    override suspend fun trending(type: MediaType): Result<List<Title>> = safeCall {
        when (type) {
            MediaType.MOVIE -> api.trending().results.map { it.toDomain() }
            MediaType.TV -> api.tvTrending().results.map { it.toDomain() }
        }
    }

    override suspend fun recommendationsFor(key: TitleKey): Result<List<Title>> = safeCall {
        when (key.type) {
            MediaType.MOVIE -> api.recommendations(key.id).results.map { it.toDomain() }
            MediaType.TV -> api.tvRecommendations(key.id).results.map { it.toDomain() }
        }
    }

    override suspend fun discoverByGenre(type: MediaType, genreId: Int, page: Int): Result<List<Title>> = safeCall {
        when (type) {
            MediaType.MOVIE -> api.discover(genreIds = genreId.toString(), page = page).results.map { it.toDomain() }
            MediaType.TV -> {
                val tvGenre = GenreMap.tvGenreFor(genreId)
                if (tvGenre == null) emptyList()
                else api.discoverTv(genreIds = tvGenre.toString(), page = page).results.map { it.toDomain() }
            }
        }
    }

    override suspend fun search(query: String, page: Int): Result<SearchResults> =
        safeCall { api.searchMulti(query, page).toSearchResults() }

    override suspend fun details(key: TitleKey, region: String): Result<TitleDetails> = safeCall {
        when (key.type) {
            MediaType.MOVIE -> api.details(key.id).toDomain(region)
            MediaType.TV -> api.tvDetails(key.id).toDomain(region)
        }
    }

    override suspend fun person(id: Int): Result<Person> = safeCall { api.person(id).toDomain() }
}
