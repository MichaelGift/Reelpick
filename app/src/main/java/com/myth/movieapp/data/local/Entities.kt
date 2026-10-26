package com.myth.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed

// Movies are stored as JSON columns (see Converters): nothing queries their fields, and it keeps
// the schema stable while the domain model evolves.

/** One signal per movie; the latest one wins. */
@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey val movieId: Int,
    val type: FeedbackType,
    val timestampMillis: Long,
    val movie: Movie,
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val movieId: Int,
    val addedAtMillis: Long,
    val movie: Movie,
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

/** Single-row table holding the last ranked feed. */
@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val id: Int = 0,
    val feed: ForYouFeed,
)
