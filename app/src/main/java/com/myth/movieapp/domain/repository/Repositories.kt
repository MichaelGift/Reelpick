package com.myth.movieapp.domain.repository

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.SearchResults
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.PagedTitles
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.ForYouFeed
import kotlinx.coroutines.flow.Flow

/** Read-only access to TMDB. Every call returns a [Result] instead of throwing. */
interface TitleRepository {
    /** Movie genres plus the TV-only ones, in one id space (see GenreMap). */
    suspend fun genres(): Result<List<Genre>>
    suspend fun popular(type: MediaType = MediaType.MOVIE, page: Int = 1): Result<PagedTitles>
    suspend fun topRated(type: MediaType = MediaType.MOVIE, page: Int = 1): Result<PagedTitles>
    suspend fun trending(type: MediaType = MediaType.MOVIE): Result<List<Title>>
    /** Recommendations of the same type as [key]: series for a series, films for a film. */
    suspend fun recommendationsFor(key: TitleKey): Result<List<Title>>
    /** Empty (not a failure) when [type] has no equivalent of [genreId], e.g. Horror for TV. */
    suspend fun discoverByGenre(type: MediaType, genreId: Int, page: Int = 1): Result<List<Title>>
    /** Films, series and people matching [query]. */
    suspend fun search(query: String, page: Int = 1): Result<SearchResults>
    suspend fun details(key: TitleKey, region: String): Result<TitleDetails>
    suspend fun person(id: Int): Result<Person>
}

/** The user's explicit signals about titles: likes, dislikes, "not interested" and watched. */
interface FeedbackRepository {
    val feedback: Flow<List<Feedback>>
    fun feedbackFor(key: TitleKey): Flow<FeedbackType?>
    suspend fun setFeedback(title: Title, type: FeedbackType)
    suspend fun clearFeedback(key: TitleKey)
    suspend fun clearAll()
}

interface WatchlistRepository {
    val watchlist: Flow<List<Title>>
    fun isInWatchlist(key: TitleKey): Flow<Boolean>
    suspend fun add(title: Title)
    suspend fun remove(key: TitleKey)
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding(genreIds: Set<Int>)
    suspend fun resetOnboarding()
    suspend fun setRegion(region: String?)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun setFeedScope(scope: FeedScope)
}

/** Last computed For You feed per scope, so the home screen has something to show offline. */
interface FeedCacheRepository {
    fun cachedFeed(scope: FeedScope): Flow<ForYouFeed?>
    suspend fun save(scope: FeedScope, feed: ForYouFeed)
    suspend fun clear()
}
