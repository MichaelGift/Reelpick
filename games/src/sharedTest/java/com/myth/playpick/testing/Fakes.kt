package com.myth.playpick.testing

import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Checklist
import com.myth.playpick.domain.model.ChecklistItem
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
import com.myth.playpick.domain.model.Progress
import com.myth.playpick.domain.model.ThemeMode
import com.myth.playpick.domain.model.UserPreferences
import com.myth.playpick.domain.repository.ChecklistRepository
import com.myth.playpick.domain.repository.FeedCacheRepository
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

val PC = Named(1, "PC", "pc")
val NINTENDO = Named(7, "Nintendo", "nintendo")

fun game(
    id: Int,
    genres: List<Int> = emptyList(),
    tags: List<Int> = emptyList(),
    platforms: List<Named> = listOf(PC),
    rating: Double = 4.0,
    ratingsCount: Int = 1_000,
    released: String? = "2020-01-01",
    playtime: Int = 20,
    name: String = "Game $id",
) = Game(
    id = id,
    slug = "game-$id",
    name = name,
    coverUrl = "https://media.rawg.io/media/games/$id.jpg",
    released = released,
    rating = rating,
    ratingsCount = ratingsCount,
    playtimeHours = playtime,
    genres = genres.map { Named(it, "Genre $it", "genre-$it") },
    tags = tags.map { Named(it, "Tag $it", "tag-$it") },
    platforms = platforms,
)

/**
 * RAWG in memory. `games` answers by the first rule whose predicate matches the query; series,
 * developers and achievements are keyed by game id. [failing] makes every call fail.
 */
class FakeGameRepository : GameRepository {
    val gameRules = mutableListOf<Pair<(GameQuery) -> Boolean, List<Game>>>()
    val seriesById = mutableMapOf<Int, List<Game>>()
    val additionsById = mutableMapOf<Int, List<Game>>()
    val developersById = mutableMapOf<Int, List<Named>>()
    val achievementsById = mutableMapOf<Int, List<Achievement>>()
    val queries = mutableListOf<GameQuery>()
    var failing = false
    var genreList = listOf(Named(4, "Action", "action"), Named(5, "RPG", "role-playing-games-rpg"), Named(51, "Indie", "indie"))
    var tagList = listOf(Named(259, "Metroidvania", "metroidvania"), Named(49, "Difficult", "difficult"))

    fun whenQuery(predicate: (GameQuery) -> Boolean, games: List<Game>) {
        gameRules += predicate to games
    }

    private fun <T> answer(value: () -> T): Result<T> = if (failing) Result.failure(IOException("offline")) else Result.success(value())

    override suspend fun games(query: GameQuery): Result<Paged<Game>> = answer {
        queries += query
        Paged(gameRules.firstOrNull { it.first(query) }?.second.orEmpty(), query.page, hasMore = false)
    }

    override suspend fun details(id: Int): Result<GameDetails> = answer {
        GameDetails(
            game = game(id),
            description = "About game $id",
            developers = developersById[id].orEmpty(),
            publishers = emptyList(),
            website = null,
            stores = emptyList(),
            screenshots = emptyList(),
            achievementCount = achievementsById[id]?.size ?: 0,
            seriesCount = seriesById[id]?.size ?: 0,
            additionsCount = additionsById[id]?.size ?: 0,
        )
    }

    override suspend fun series(id: Int) = answer { seriesById[id].orEmpty() }
    override suspend fun additions(id: Int) = answer { additionsById[id].orEmpty() }
    override suspend fun achievements(id: Int) = answer { achievementsById[id].orEmpty() }
    override suspend fun genres() = answer { genreList }
    override suspend fun tags() = answer { tagList }
}

class FakeFeedbackRepository(initial: List<Feedback> = emptyList()) : FeedbackRepository {
    private val state = MutableStateFlow(initial)
    private var clock = 10_000L
    override val feedback: Flow<List<Feedback>> = state

    override fun feedbackFor(gameId: Int): Flow<FeedbackType?> = state.map { list -> list.firstOrNull { it.game.id == gameId }?.type }

    override suspend fun setFeedback(game: Game, type: FeedbackType?) {
        state.update { list ->
            val rest = list.filterNot { it.game.id == game.id }
            if (type == null) rest else listOf(Feedback(game, type, clock++)) + rest
        }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakeLibraryRepository(initial: List<LibraryEntry> = emptyList()) : LibraryRepository {
    val state = MutableStateFlow(initial)
    private var clock = 20_000L
    override val entries: Flow<List<LibraryEntry>> = state.map { list -> list.sortedByDescending { it.updatedAtMillis } }

    override fun entry(gameId: Int): Flow<LibraryEntry?> = state.map { list -> list.firstOrNull { it.game.id == gameId } }

    override suspend fun setStatus(game: Game, status: LibraryStatus) {
        state.update { list ->
            val existing = list.firstOrNull { it.game.id == game.id }
            val now = clock++
            listOf(
                LibraryEntry(game, status, existing?.addedAtMillis ?: now, now, existing?.progress ?: Progress(0, 0)),
            ) + list.filterNot { it.game.id == game.id }
        }
    }

    override suspend fun remove(gameId: Int) {
        state.update { list -> list.filterNot { it.game.id == gameId } }
    }
}

fun entry(game: Game, status: LibraryStatus, updatedAt: Long = 1L, progress: Progress = Progress(0, 0)) =
    LibraryEntry(game, status, updatedAt, updatedAt, progress)

class FakeChecklistRepository(private val games: GameRepository) : ChecklistRepository {
    private val lists = MutableStateFlow<Map<Int, Checklist>>(emptyMap())
    var syncs = 0

    override fun checklist(gameId: Int): Flow<Checklist> = lists.map { it[gameId] ?: Checklist(gameId, emptyList(), null) }

    override suspend fun sync(gameId: Int): Result<Unit> = games.achievements(gameId).map { achievements ->
        syncs++
        lists.update { all ->
            val ticks = all[gameId]?.items.orEmpty().filter { it.unlocked }.associate { it.achievement.id to it.unlockedAtMillis }
            all + (gameId to Checklist(gameId, achievements.map { ChecklistItem(it, ticks[it.id]) }, syncedAtMillis = 1L))
        }
    }

    override suspend fun setUnlocked(gameId: Int, achievementId: Long, unlocked: Boolean) {
        lists.update { all ->
            val list = all[gameId] ?: return@update all
            all + (gameId to list.copy(items = list.items.map {
                if (it.achievement.id == achievementId) it.copy(unlockedAtMillis = if (unlocked) 99L else null) else it
            }))
        }
    }
}

class FakePreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    override suspend fun completeOnboarding(platforms: Set<Platform>, genreIds: Set<Int>, tagIds: Set<Int>) {
        state.update { it.copy(onboardingComplete = true, platforms = platforms, genreIds = genreIds, tagIds = tagIds) }
    }

    override suspend fun resetOnboarding() {
        state.update { it.copy(onboardingComplete = false, genreIds = emptySet(), tagIds = emptySet(), feedPlatform = null) }
    }

    override suspend fun setPlatforms(platforms: Set<Platform>) = state.update { it.copy(platforms = platforms) }
    override suspend fun setFeedPlatform(platform: Platform?) = state.update { it.copy(feedPlatform = platform) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun addRecentSearch(query: String) = state.update { it.copy(recentSearches = (listOf(query) + it.recentSearches).distinct()) }
    override suspend fun clearRecentSearches() = state.update { it.copy(recentSearches = emptyList()) }
}

class FakeFeedCacheRepository : FeedCacheRepository {
    val saved = mutableMapOf<String, String>()
    override suspend fun cached(scope: String) = saved[scope]
    override suspend fun save(scope: String, json: String) {
        saved[scope] = json
    }
    override suspend fun clear() = saved.clear()
}
