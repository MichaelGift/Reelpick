package com.myth.movieapp.ui.common

import com.myth.movieapp.data.remote.MissingTmdbTokenException
import com.myth.movieapp.domain.recommend.RecommendationReason
import retrofit2.HttpException
import java.io.IOException

object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"
    fun poster(path: String?) = path?.let { "${BASE}w342$it" }
    fun posterLarge(path: String?) = path?.let { "${BASE}w780$it" }
    fun backdrop(path: String?) = path?.let { "${BASE}w1280$it" }
    fun profile(path: String?) = path?.let { "${BASE}w185$it" }
    fun logo(path: String?) = path?.let { "${BASE}w92$it" }
}

fun Throwable.toUserMessage(): String = when (this) {
    is MissingTmdbTokenException ->
        "No TMDB token configured. Add TMDB_ACCESS_TOKEN to local.properties and rebuild."
    is HttpException -> when (code()) {
        401 -> "TMDB rejected the access token. Check TMDB_ACCESS_TOKEN."
        404 -> "That movie couldn't be found."
        429 -> "Too many requests to TMDB. Try again in a moment."
        else -> "TMDB returned an error (${code()})."
    }
    is IOException -> "Couldn't reach TMDB. Check your connection."
    else -> "Something went wrong."
}

fun RecommendationReason.label(): String = when (this) {
    is RecommendationReason.BecauseYouLiked -> "Because you liked $seedTitle"
    is RecommendationReason.MatchesGenres -> "For ${genreNames.joinToString(" & ")} fans"
    RecommendationReason.Trending -> "Trending this week"
}
