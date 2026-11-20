package com.myth.movieapp.data.local

import androidx.room.TypeConverter
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.recommend.ForYouFeed
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun titleToJson(movie: Title): String = json.encodeToString(Title.serializer(), movie)

    @TypeConverter
    fun jsonToTitle(value: String): Title = json.decodeFromString(Title.serializer(), value)

    @TypeConverter
    fun feedToJson(feed: ForYouFeed): String = json.encodeToString(ForYouFeed.serializer(), feed)

    @TypeConverter
    fun jsonToFeed(value: String): ForYouFeed = json.decodeFromString(ForYouFeed.serializer(), value)
}
