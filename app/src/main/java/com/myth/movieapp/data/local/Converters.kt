package com.myth.movieapp.data.local

import androidx.room.TypeConverter
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun movieToJson(movie: Movie): String = json.encodeToString(Movie.serializer(), movie)

    @TypeConverter
    fun jsonToMovie(value: String): Movie = json.decodeFromString(Movie.serializer(), value)

    @TypeConverter
    fun feedToJson(feed: ForYouFeed): String = json.encodeToString(ForYouFeed.serializer(), feed)

    @TypeConverter
    fun jsonToFeed(value: String): ForYouFeed = json.decodeFromString(ForYouFeed.serializer(), value)
}
