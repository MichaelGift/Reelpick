package com.myth.movieapp.domain.repository

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.PagedTitles
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.ForYouFeed
import kotlinx.coroutines.flow.Flow

/** Read-only access to TMDB. Every call returns a [Result] instead of throwing. */
interface TitleRepository {
    suspend fun genres(): Result<List<Genre>>
    suspend fun popular(page: Int = 1): Result<PagedTitles>
    suspend fun topRated(page: Int = 1): Result<PagedTitles>
    suspend fun trending(): Result<List<Title>>
    suspend fun recommendationsFor(movieId: Int): Result<List<Title>>
    suspend fun discoverByGenre(genreId: Int, page: Int = 1): Result<List<Title>>
    suspend fun search(query: String, page: Int = 1): Result<PagedTitles>
    suspend fun details(movieId: Int, region: String): Result<TitleDetails>
}

/** The user's explicit signals about movies: likes, dislikes, "not interested" and watched. */
interface FeedbackRepository {
    val feedback: Flow<List<Feedback>>
    fun feedbackFor(movieId: Int): Flow<FeedbackType?>
    suspend fun setFeedback(movie: Title, type: FeedbackType)
    suspend fun clearFeedback(movieId: Int)
    suspend fun clearAll()
}

interface WatchlistRepository {
    val watchlist: Flow<List<Title>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    suspend fun add(movie: Title)
    suspend fun remove(movieId: Int)
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding(genreIds: Set<Int>)
    suspend fun resetOnboarding()
    suspend fun setRegion(region: String?)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

/** Last computed For You feed, so the home screen has something to show offline. */
interface FeedCacheRepository {
    val cachedFeed: Flow<ForYouFeed?>
    suspend fun save(feed: ForYouFeed)
    suspend fun clear()
}
