package com.myth.movieapp.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Movie(
    val id: Int,
    val title: String,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
) {
    val releaseYear: String? get() = releaseDate?.take(4)?.takeIf { it.length == 4 }

    fun releaseLocalDate(): LocalDate? =
        releaseDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

@Serializable
data class Genre(val id: Int, val name: String)

data class MovieDetails(
    val movie: Movie,
    val tagline: String?,
    val runtimeMinutes: Int?,
    val genres: List<Genre>,
    val cast: List<CastMember>,
    val trailerYoutubeKey: String?,
    /** Watch providers for the requested region, or null when TMDB/JustWatch has none. */
    val watchProviders: WatchProviders?,
)

data class CastMember(val id: Int, val name: String, val character: String?, val profilePath: String?)

data class WatchProviders(
    val link: String?,
    val stream: List<WatchProvider>,
    val rent: List<WatchProvider>,
    val buy: List<WatchProvider>,
) {
    val isEmpty: Boolean get() = stream.isEmpty() && rent.isEmpty() && buy.isEmpty()
}

data class WatchProvider(val id: Int, val name: String, val logoPath: String?)

enum class FeedbackType { LIKE, DISLIKE, NOT_INTERESTED, WATCHED }

data class Feedback(val movie: Movie, val type: FeedbackType, val timestampMillis: Long)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val selectedGenreIds: Set<Int> = emptySet(),
    /** ISO 3166-1 country code used for watch providers; null means "use the device locale". */
    val region: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val recentSearches: List<String> = emptyList(),
)

data class PagedMovies(val movies: List<Movie>, val page: Int, val totalPages: Int) {
    val hasMore: Boolean get() = page < totalPages
}
