package com.myth.movieapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.myth.movieapp.domain.model.FeedbackType
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<FeedbackEntity>>

    @Query("SELECT type FROM feedback WHERE movieId = :movieId")
    fun observeType(movieId: Int): Flow<FeedbackType?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FeedbackEntity)

    @Query("DELETE FROM feedback WHERE movieId = :movieId")
    suspend fun delete(movieId: Int)

    @Query("DELETE FROM feedback")
    suspend fun deleteAll()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId)")
    fun observeContains(movieId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE movieId = :movieId")
    suspend fun delete(movieId: Int)
}

@Dao
interface GenreDao {
    @Query("SELECT * FROM genres ORDER BY name")
    suspend fun getAll(): List<GenreEntity>

    @Transaction
    suspend fun replaceAll(genres: List<GenreEntity>) {
        deleteAll()
        insertAll(genres)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(genres: List<GenreEntity>)

    @Query("DELETE FROM genres")
    suspend fun deleteAll()
}

@Dao
interface FeedCacheDao {
    @Query("SELECT * FROM feed_cache WHERE id = 0")
    fun observe(): Flow<FeedCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FeedCacheEntity)

    @Query("DELETE FROM feed_cache")
    suspend fun clear()
}
