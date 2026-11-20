package com.myth.playpick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FeedbackEntity::class,
        LibraryEntity::class,
        AchievementEntity::class,
        AchievementProgressEntity::class,
        ChecklistSyncEntity::class,
        FeedCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedbackDao(): FeedbackDao
    abstract fun libraryDao(): LibraryDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun feedCacheDao(): FeedCacheDao
}
