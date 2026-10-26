package com.myth.movieapp.testing

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.PagedMovies
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.MovieRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

fun movie(
    id: Int,
    genres: List<Int> = emptyList(),
    voteAverage: Double = 7.0,
    voteCount: Int = 1_000,
    releaseDate: String? = "2020-01-01",
    title: String = "Movie $id",
) = Movie(
    id = id,
    title = title,
    genreIds = genres,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
)

class FakeMovieRepository : MovieRepository {
    var genres: List<Genre> = emptyList()
    var trending: Result<List<Movie>> = Result.success(emptyList())
    val recommendations = mutableMapOf<Int, Result<List<Movie>>>()
    val discover = mutableMapOf<Int, Result<List<Movie>>>()
    val details = mutableMapOf<Int, Result<MovieDetails>>()
    var searchResults: (String, Int) -> PagedMovies = { _, page -> PagedMovies(emptyList(), page, page) }
    val searchQueries = mutableListOf<String>()
    var fetchCount = 0
        private set

    var popular: List<Movie> = emptyList()

    override suspend fun genres() = Result.success(genres)
    override suspend fun popular(page: Int) = Result.success(PagedMovies(popular, 1, 1))
    override suspend fun topRated(page: Int) = Result.success(PagedMovies(emptyList(), 1, 1))
    override suspend fun trending(): Result<List<Movie>> = trending.also { fetchCount++ }
    override suspend fun recommendationsFor(movieId: Int) =
        recommendations[movieId] ?: Result.success(emptyList())
    override suspend fun discoverByGenre(genreId: Int, page: Int) =
        discover[genreId] ?: Result.success(emptyList())
    override suspend fun search(query: String, page: Int): Result<PagedMovies> {
        searchQueries += query
        return Result.success(searchResults(query, page))
    }
    override suspend fun details(movieId: Int, region: String): Result<MovieDetails> =
        details[movieId] ?: Result.failure(IOException("not faked"))
}

class FakeFeedbackRepository : FeedbackRepository {
    private val state = MutableStateFlow<Map<Int, Feedback>>(emptyMap())
    private var tick = 0L

    override val feedback: Flow<List<Feedback>> =
        state.map { it.values.sortedByDescending(Feedback::timestampMillis) }

    override fun feedbackFor(movieId: Int) = state.map { it[movieId]?.type }

    override suspend fun setFeedback(movie: Movie, type: FeedbackType) =
        state.update { it + (movie.id to Feedback(movie, type, ++tick)) }

    override suspend fun clearFeedback(movieId: Int) = state.update { it - movieId }
    override suspend fun clearAll() = state.update { emptyMap() }
}

class FakeWatchlistRepository : WatchlistRepository {
    private val state = MutableStateFlow<List<Movie>>(emptyList())
    override val watchlist: Flow<List<Movie>> = state
    override fun isInWatchlist(movieId: Int) = state.map { list -> list.any { it.id == movieId } }
    override suspend fun add(movie: Movie) = state.update { listOf(movie) + it.filterNot { m -> m.id == movie.id } }
    override suspend fun remove(movieId: Int) = state.update { it.filterNot { m -> m.id == movieId } }
}

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state
    override suspend fun completeOnboarding(genreIds: Set<Int>) =
        state.update { it.copy(onboardingComplete = true, selectedGenreIds = genreIds) }
    override suspend fun resetOnboarding() =
        state.update { it.copy(onboardingComplete = false, selectedGenreIds = emptySet()) }
    override suspend fun setRegion(region: String?) = state.update { it.copy(region = region) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun addRecentSearch(query: String) =
        state.update { it.copy(recentSearches = listOf(query) + it.recentSearches) }
    override suspend fun clearRecentSearches() = state.update { it.copy(recentSearches = emptyList()) }
}

class FakeFeedCacheRepository : FeedCacheRepository {
    private val state = MutableStateFlow<ForYouFeed?>(null)
    override val cachedFeed: Flow<ForYouFeed?> = state
    override suspend fun save(feed: ForYouFeed) { state.value = feed }
    override suspend fun clear() { state.value = null }
}
