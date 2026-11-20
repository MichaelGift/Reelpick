package com.myth.movieapp.testing

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.PagedTitles
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.SearchResults
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.TitleRepository
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
) = Title(
    id = id,
    title = title,
    mediaType = MediaType.MOVIE,
    genreIds = genres,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
)

fun series(
    id: Int,
    genres: List<Int> = emptyList(),
    voteAverage: Double = 7.0,
    voteCount: Int = 1_000,
    releaseDate: String? = "2020-01-01",
    title: String = "Series $id",
) = movie(id, genres, voteAverage, voteCount, releaseDate, title).copy(mediaType = MediaType.TV)

fun movieKey(id: Int) = TitleKey(MediaType.MOVIE, id)
fun seriesKey(id: Int) = TitleKey(MediaType.TV, id)

class FakeTitleRepository : TitleRepository {
    var genres: List<Genre> = emptyList()
    /** Trending films; [trendingSeries] for TV. */
    var trending: Result<List<Title>> = Result.success(emptyList())
    var trendingSeries: Result<List<Title>> = Result.success(emptyList())
    val recommendations = mutableMapOf<TitleKey, Result<List<Title>>>()
    /** Films by genre; [discoverSeries] for TV. */
    val discover = mutableMapOf<Int, Result<List<Title>>>()
    val discoverSeries = mutableMapOf<Int, Result<List<Title>>>()
    val details = mutableMapOf<TitleKey, Result<TitleDetails>>()
    val people = mutableMapOf<Int, Result<Person>>()
    var searchResults: (String, Int) -> SearchResults = { _, page -> SearchResults(emptyList(), emptyList(), page, page) }
    val searchQueries = mutableListOf<String>()
    /** How many times candidates were fetched from the network (counted via trending calls). */
    var fetchCount = 0
        private set

    var popular: List<Title> = emptyList()
    var popularSeries: List<Title> = emptyList()

    override suspend fun genres() = Result.success(genres)

    override suspend fun popular(type: MediaType, page: Int) =
        Result.success(PagedTitles(if (type == MediaType.MOVIE) popular else popularSeries, 1, 1))

    override suspend fun topRated(type: MediaType, page: Int) = Result.success(PagedTitles(emptyList(), 1, 1))

    override suspend fun trending(type: MediaType): Result<List<Title>> {
        if (type == MediaType.MOVIE) fetchCount++
        return if (type == MediaType.MOVIE) trending else trendingSeries
    }

    override suspend fun recommendationsFor(key: TitleKey) = recommendations[key] ?: Result.success(emptyList())

    override suspend fun discoverByGenre(type: MediaType, genreId: Int, page: Int) =
        (if (type == MediaType.MOVIE) discover[genreId] else discoverSeries[genreId]) ?: Result.success(emptyList())

    override suspend fun search(query: String, page: Int): Result<SearchResults> {
        searchQueries += query
        return Result.success(searchResults(query, page))
    }

    override suspend fun details(key: TitleKey, region: String): Result<TitleDetails> =
        details[key] ?: Result.failure(IOException("not faked"))

    override suspend fun person(id: Int): Result<Person> = people[id] ?: Result.failure(IOException("not faked"))
}

class FakeFeedbackRepository : FeedbackRepository {
    private val state = MutableStateFlow<Map<TitleKey, Feedback>>(emptyMap())
    private var tick = 0L

    override val feedback: Flow<List<Feedback>> =
        state.map { it.values.sortedByDescending(Feedback::timestampMillis) }

    override fun feedbackFor(key: TitleKey) = state.map { it[key]?.type }

    override suspend fun setFeedback(title: Title, type: FeedbackType) =
        state.update { it + (title.key to Feedback(title, type, ++tick)) }

    override suspend fun clearFeedback(key: TitleKey) = state.update { it - key }
    override suspend fun clearAll() = state.update { emptyMap() }
}

class FakeWatchlistRepository : WatchlistRepository {
    private val state = MutableStateFlow<List<Title>>(emptyList())
    override val watchlist: Flow<List<Title>> = state
    override fun isInWatchlist(key: TitleKey) = state.map { list -> list.any { it.key == key } }
    override suspend fun add(title: Title) = state.update { listOf(title) + it.filterNot { t -> t.key == title.key } }
    override suspend fun remove(key: TitleKey) = state.update { it.filterNot { t -> t.key == key } }
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
