package com.myth.playpick.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FeedbackEntity>>

    @Query("SELECT type FROM feedback WHERE gameId = :gameId")
    fun observeType(gameId: Int): Flow<String?>

    @Upsert
    suspend fun upsert(entity: FeedbackEntity)

    @Query("DELETE FROM feedback WHERE gameId = :gameId")
    suspend fun delete(gameId: Int)

    @Query("DELETE FROM feedback")
    suspend fun clear()
}

/** Progress counts only ticks whose achievement is still in the current list. */
private const val LIBRARY_ROW = """
    SELECT l.*,
        (SELECT COUNT(*) FROM achievement a WHERE a.gameId = l.gameId) AS total,
        (SELECT COUNT(*) FROM achievement_progress p
            JOIN achievement a ON a.gameId = p.gameId AND a.achievementId = p.achievementId
            WHERE p.gameId = l.gameId) AS unlocked
    FROM library l
"""

@Dao
interface LibraryDao {
    @Query("$LIBRARY_ROW ORDER BY l.updatedAt DESC")
    fun observeAll(): Flow<List<LibraryRow>>

    @Query("$LIBRARY_ROW WHERE l.gameId = :gameId")
    fun observe(gameId: Int): Flow<LibraryRow?>

    @Query("SELECT * FROM library WHERE gameId = :gameId")
    suspend fun get(gameId: Int): LibraryEntity?

    @Upsert
    suspend fun upsert(entity: LibraryEntity)

    @Query("DELETE FROM library WHERE gameId = :gameId")
    suspend fun delete(gameId: Int)
}

@Dao
interface ChecklistDao {
    @Query(
        """
        SELECT a.*, p.unlockedAt FROM achievement a
        LEFT JOIN achievement_progress p ON p.gameId = a.gameId AND p.achievementId = a.achievementId
        WHERE a.gameId = :gameId
        ORDER BY a.position
        """,
    )
    fun observe(gameId: Int): Flow<List<ChecklistRow>>

    @Query("SELECT syncedAt FROM checklist_sync WHERE gameId = :gameId")
    fun observeSyncedAt(gameId: Int): Flow<Long?>

    @Query("DELETE FROM achievement WHERE gameId = :gameId")
    suspend fun deleteAchievements(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(items: List<AchievementEntity>)

    @Upsert
    suspend fun upsertSync(entity: ChecklistSyncEntity)

    /** Swaps in a fresh list; ticks live in another table and are untouched. */
    @Transaction
    suspend fun replace(gameId: Int, items: List<AchievementEntity>, syncedAt: Long) {
        deleteAchievements(gameId)
        insertAchievements(items)
        upsertSync(ChecklistSyncEntity(gameId, syncedAt))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entity: AchievementProgressEntity)

    @Query("DELETE FROM achievement_progress WHERE gameId = :gameId AND achievementId = :achievementId")
    suspend fun lock(gameId: Int, achievementId: Long)
}

@Dao
interface FeedCacheDao {
    @Query("SELECT * FROM feed_cache WHERE scope = :scope")
    suspend fun get(scope: String): FeedCacheEntity?

    @Upsert
    suspend fun upsert(entity: FeedCacheEntity)

    @Query("DELETE FROM feed_cache")
    suspend fun clear()
}
