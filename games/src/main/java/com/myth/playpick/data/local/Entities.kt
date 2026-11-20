package com.myth.playpick.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A like, dislike or "not interested". The game is stored as JSON so the profile can be rebuilt offline. */
@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey val gameId: Int,
    val type: String,
    val gameJson: String,
    val createdAt: Long,
)

@Entity(tableName = "library", indices = [Index("status")])
data class LibraryEntity(
    @PrimaryKey val gameId: Int,
    val status: String,
    val gameJson: String,
    val addedAt: Long,
    val updatedAt: Long,
)

/** RAWG's achievement list for a game, replaced wholesale on every sync. */
@Entity(tableName = "achievement", primaryKeys = ["gameId", "achievementId"])
data class AchievementEntity(
    val gameId: Int,
    val achievementId: Long,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val percent: Double?,
    /** RAWG's own order, kept for the default sort. */
    val position: Int,
)

/**
 * The user's ticks, kept apart from [AchievementEntity] so re-syncing a list can never lose them.
 * A tick whose achievement disappears from RAWG simply stops counting.
 */
@Entity(tableName = "achievement_progress", primaryKeys = ["gameId", "achievementId"])
data class AchievementProgressEntity(
    val gameId: Int,
    val achievementId: Long,
    val unlockedAt: Long,
)

/** When a game's achievement list was last fetched, so a game with none isn't refetched on every open. */
@Entity(tableName = "checklist_sync")
data class ChecklistSyncEntity(
    @PrimaryKey val gameId: Int,
    val syncedAt: Long,
)

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val scope: String,
    val json: String,
    val generatedAt: Long,
)

data class LibraryRow(
    @Embedded val entry: LibraryEntity,
    val total: Int,
    val unlocked: Int,
)

data class ChecklistRow(
    @Embedded val achievement: AchievementEntity,
    val unlockedAt: Long?,
)
