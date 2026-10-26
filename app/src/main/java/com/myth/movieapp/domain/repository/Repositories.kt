package com.myth.movieapp.domain.repository

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.PagedMovies
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.ForYouFeed
import kotlinx.coroutines.flow.Flow

/** Read-only access to TMDB. Every call returns a [Result] instead of throwing. */
interface MovieRepository {
    suspend fun genres(): Result<List<Genre>>
    suspend fun popular(page: Int = 1): Result<PagedMovies>
    suspend fun topRated(page: Int = 1): Result<PagedMovies>
    suspend fun trending(): Result<List<Movie>>
    suspend fun recommendationsFor(movieId: Int): Result<List<Movie>>
    suspend fun discoverByGenre(genreId: Int, page: Int = 1): Result<List<Movie>>
    suspend fun search(query: String, page: Int = 1): Result<PagedMovies>
    suspend fun details(movieId: Int, region: String): Result<MovieDetails>
}

/** The user's explicit signals about movies: likes, dislikes, "not interested" and watched. */
interface FeedbackRepository {
    val feedback: Flow<List<Feedback>>
    fun feedbackFor(movieId: Int): Flow<FeedbackType?>
    suspend fun setFeedback(movie: Movie, type: FeedbackType)
    suspend fun clearFeedback(movieId: Int)
    suspend fun clearAll()
}

interface WatchlistRepository {
    val watchlist: Flow<List<Movie>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    suspend fun add(movie: Movie)
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
