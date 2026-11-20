package com.myth.movieapp.data.local

import androidx.room.TypeConverter
import com.myth.movieapp.domain.model.Title
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun titleToJson(title: Title): String = json.encodeToString(Title.serializer(), title)

    @TypeConverter
    fun jsonToTitle(value: String): Title = json.decodeFromString(Title.serializer(), value)
}
