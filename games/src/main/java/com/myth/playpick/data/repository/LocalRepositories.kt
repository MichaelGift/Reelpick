package com.myth.playpick.data.repository

import com.myth.playpick.data.local.AchievementEntity
import com.myth.playpick.data.local.AchievementProgressEntity
import com.myth.playpick.data.local.ChecklistDao
import com.myth.playpick.data.local.FeedCacheDao
import com.myth.playpick.data.local.FeedCacheEntity
import com.myth.playpick.data.local.FeedbackDao
import com.myth.playpick.data.local.FeedbackEntity
import com.myth.playpick.data.local.LibraryDao
import com.myth.playpick.data.local.LibraryEntity
import com.myth.playpick.data.local.LibraryRow
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Checklist
import com.myth.playpick.domain.model.ChecklistItem
import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Progress
import com.myth.playpick.domain.repository.ChecklistRepository
import com.myth.playpick.domain.repository.FeedCacheRepository
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Rows written by an older build may not decode; they're skipped rather than crashing a screen. */
private fun Json.decodeGame(json: String): Game? = runCatching { decodeFromString(Game.serializer(), json) }.getOrNull()

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val dao: FeedbackDao,
    private val json: Json,
    private val clock: Clock,
) : FeedbackRepository {

    override val feedback: Flow<List<Feedback>> = dao.observeAll().map { rows ->
        rows.mapNotNull { row ->
            val game = json.decodeGame(row.gameJson) ?: return@mapNotNull null
            val type = enumOrNull<FeedbackType>(row.type) ?: return@mapNotNull null
            Feedback(game, type, row.createdAt)
        }
    }

    override fun feedbackFor(gameId: Int): Flow<FeedbackType?> =
        dao.observeType(gameId).map { it?.let { name -> enumOrNull<FeedbackType>(name) } }.distinctUntilChanged()

    override suspend fun setFeedback(game: Game, type: FeedbackType?) {
        if (type == null) {
            dao.delete(game.id)
        } else {
            dao.upsert(FeedbackEntity(game.id, type.name, json.encodeToString(Game.serializer(), game), clock.millis()))
        }
    }

    override suspend fun clear() = dao.clear()
}

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
    private val json: Json,
    private val clock: Clock,
) : LibraryRepository {

    override val entries: Flow<List<LibraryEntry>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun entry(gameId: Int): Flow<LibraryEntry?> = dao.observe(gameId).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun setStatus(game: Game, status: LibraryStatus) {
        val now = clock.millis()
        val existing = dao.get(game.id)
        dao.upsert(
            LibraryEntity(
                gameId = game.id,
                status = status.name,
                gameJson = json.encodeToString(Game.serializer(), game),
                addedAt = existing?.addedAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun remove(gameId: Int) = dao.delete(gameId)

    private fun LibraryRow.toDomain(): LibraryEntry? {
        val game = json.decodeGame(entry.gameJson) ?: return null
        val status = enumOrNull<LibraryStatus>(entry.status) ?: return null
        return LibraryEntry(game, status, entry.addedAt, entry.updatedAt, Progress(unlocked, total))
    }
}

@Singleton
class ChecklistRepositoryImpl @Inject constructor(
    private val dao: ChecklistDao,
    private val games: GameRepository,
    private val clock: Clock,
) : ChecklistRepository {

    override fun checklist(gameId: Int): Flow<Checklist> =
        combine(dao.observe(gameId), dao.observeSyncedAt(gameId)) { rows, syncedAt ->
            Checklist(
                gameId = gameId,
                items = rows.map { row ->
                    val a = row.achievement
                    ChecklistItem(Achievement(a.achievementId, a.name, a.description, a.iconUrl, a.percent), row.unlockedAt)
                },
                syncedAtMillis = syncedAt,
            )
        }.distinctUntilChanged()

    override suspend fun sync(gameId: Int): Result<Unit> = games.achievements(gameId).map { achievements ->
        val entities = achievements.mapIndexed { index, a ->
            AchievementEntity(gameId, a.id, a.name, a.description, a.iconUrl, a.percent, index)
        }
        dao.replace(gameId, entities, clock.millis())
    }

    override suspend fun setUnlocked(gameId: Int, achievementId: Long, unlocked: Boolean) {
        if (unlocked) dao.unlock(AchievementProgressEntity(gameId, achievementId, clock.millis())) else dao.lock(gameId, achievementId)
    }
}

@Singleton
class FeedCacheRepositoryImpl @Inject constructor(
    private val dao: FeedCacheDao,
    private val clock: Clock,
) : FeedCacheRepository {
    override suspend fun cached(scope: String): String? = dao.get(scope)?.json
    override suspend fun save(scope: String, json: String) = dao.upsert(FeedCacheEntity(scope, json, clock.millis()))
    override suspend fun clear() = dao.clear()
}
