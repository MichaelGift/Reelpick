package com.myth.playpick.domain.repository

import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Checklist
import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameDetails
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Paged
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.ThemeMode
import com.myth.playpick.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** RAWG, behind `Result` so callers handle failure explicitly. */
interface GameRepository {
    suspend fun games(query: GameQuery): Result<Paged<Game>>
    suspend fun details(id: Int): Result<GameDetails>
    suspend fun series(id: Int): Result<List<Game>>
    suspend fun additions(id: Int): Result<List<Game>>
    /** Every achievement, following RAWG's pagination. */
    suspend fun achievements(id: Int): Result<List<Achievement>>
    suspend fun genres(): Result<List<Named>>
    /** Popular taste tags for onboarding, storefront tags removed. */
    suspend fun tags(): Result<List<Named>>
}

interface FeedbackRepository {
    val feedback: Flow<List<Feedback>>
    fun feedbackFor(gameId: Int): Flow<FeedbackType?>
    suspend fun setFeedback(game: Game, type: FeedbackType?)
    suspend fun clear()
}

interface LibraryRepository {
    /** Most recently updated first, with checklist progress. */
    val entries: Flow<List<LibraryEntry>>
    fun entry(gameId: Int): Flow<LibraryEntry?>
    suspend fun setStatus(game: Game, status: LibraryStatus)
    suspend fun remove(gameId: Int)
}

interface ChecklistRepository {
    fun checklist(gameId: Int): Flow<Checklist>

    /** Fetches the achievement list and stores it; existing ticks survive, matched by achievement id. */
    suspend fun sync(gameId: Int): Result<Unit>
    suspend fun setUnlocked(gameId: Int, achievementId: Long, unlocked: Boolean)
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding(platforms: Set<Platform>, genreIds: Set<Int>, tagIds: Set<Int>)
    suspend fun resetOnboarding()
    suspend fun setPlatforms(platforms: Set<Platform>)
    suspend fun setFeedPlatform(platform: Platform?)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

/** The For You feed, cached per platform scope as JSON so the home screen opens instantly and offline. */
interface FeedCacheRepository {
    suspend fun cached(scope: String): String?
    suspend fun save(scope: String, json: String)
    suspend fun clear()
}
