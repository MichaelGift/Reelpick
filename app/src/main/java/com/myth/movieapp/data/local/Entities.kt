package com.myth.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Title

// Titles are stored as JSON columns (see Converters): nothing queries their fields, and it keeps
// the schema stable while the domain model evolves. Rows are keyed by (mediaType, titleId) because
// TMDB reuses ids across movies and TV.

/** One signal per title; the latest one wins. */
@Entity(tableName = "feedback", primaryKeys = ["mediaType", "titleId"])
data class FeedbackEntity(
    val mediaType: MediaType,
    val titleId: Int,
    val type: FeedbackType,
    val timestampMillis: Long,
    val title: Title,
)

@Entity(tableName = "watchlist", primaryKeys = ["mediaType", "titleId"])
data class WatchlistEntity(
    val mediaType: MediaType,
    val titleId: Int,
    val addedAtMillis: Long,
    val title: Title,
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

/**
 * Last ranked feed per scope, as raw JSON. Decoding happens in the repository so a cache written by
 * an older app version is discarded instead of crashing the read.
 */
@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val id: Int = 0,
    val feed: String,
)
