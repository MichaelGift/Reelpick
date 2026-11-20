package com.myth.movieapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: TV support. Feedback and watchlist were keyed by a bare movie id; TMDB reuses ids across
 * movies and TV, so both tables are rebuilt with a (mediaType, titleId) key. Every existing row was
 * a movie, so it is copied across as MOVIE. Its JSON has no mediaType field and decodes to MOVIE
 * by default. The cached feed changes shape and is simply dropped; it rebuilds on launch.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `feedback_new` (`mediaType` TEXT NOT NULL, `titleId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, `timestampMillis` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                "PRIMARY KEY(`mediaType`, `titleId`))",
        )
        db.execSQL(
            "INSERT INTO `feedback_new` (`mediaType`, `titleId`, `type`, `timestampMillis`, `title`) " +
                "SELECT 'MOVIE', `movieId`, `type`, `timestampMillis`, `movie` FROM `feedback`",
        )
        db.execSQL("DROP TABLE `feedback`")
        db.execSQL("ALTER TABLE `feedback_new` RENAME TO `feedback`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `watchlist_new` (`mediaType` TEXT NOT NULL, `titleId` INTEGER NOT NULL, " +
                "`addedAtMillis` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`mediaType`, `titleId`))",
        )
        db.execSQL(
            "INSERT INTO `watchlist_new` (`mediaType`, `titleId`, `addedAtMillis`, `title`) " +
                "SELECT 'MOVIE', `movieId`, `addedAtMillis`, `movie` FROM `watchlist`",
        )
        db.execSQL("DROP TABLE `watchlist`")
        db.execSQL("ALTER TABLE `watchlist_new` RENAME TO `watchlist`")

        db.execSQL("DELETE FROM `feed_cache`")
    }
}
