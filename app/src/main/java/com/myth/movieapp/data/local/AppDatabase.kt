package com.myth.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FeedbackEntity::class, WatchlistEntity::class, GenreEntity::class, FeedCacheEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedbackDao(): FeedbackDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun genreDao(): GenreDao
    abstract fun feedCacheDao(): FeedCacheDao
}
