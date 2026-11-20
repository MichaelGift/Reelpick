package com.myth.movieapp.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class MediaType { MOVIE, TV }

/**
 * Identity of a title. TMDB numbers movies and TV series independently (movie 1399 and series 1399
 * are different titles), so an id alone is never enough.
 */
data class TitleKey(val type: MediaType, val id: Int)

/** A film or a TV series. Series map `name`/`first_air_date` onto [title]/[releaseDate]. */
@Serializable
data class Title(
    val id: Int,
    val title: String,
    // Defaulted so titles cached before TV support (always movies) still decode.
    val mediaType: MediaType = MediaType.MOVIE,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
) {
    val key: TitleKey get() = TitleKey(mediaType, id)

    val releaseYear: String? get() = releaseDate?.take(4)?.takeIf { it.length == 4 }

    fun releaseLocalDate(): LocalDate? =
        releaseDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

@Serializable
data class Genre(val id: Int, val name: String)

data class TitleDetails(
    val movie: Title,
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

data class Feedback(val movie: Title, val type: FeedbackType, val timestampMillis: Long)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val selectedGenreIds: Set<Int> = emptySet(),
    /** ISO 3166-1 country code used for watch providers; null means "use the device locale". */
    val region: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val recentSearches: List<String> = emptyList(),
)

data class PagedTitles(val movies: List<Title>, val page: Int, val totalPages: Int) {
    val hasMore: Boolean get() = page < totalPages
}
