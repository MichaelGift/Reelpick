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
    val title: Title,
    val tagline: String?,
    /** Film length, or a typical episode length for a series. */
    val runtimeMinutes: Int?,
    /** Genres as TMDB names them for this title (series use their own, e.g. "Sci-Fi & Fantasy"). */
    val genres: List<Genre>,
    val cast: List<CastMember>,
    val trailerYoutubeKey: String?,
    /** Watch providers for the requested region, or null when TMDB/JustWatch has none. */
    val watchProviders: WatchProviders?,
    /** Directors and writers for films; series list their creators in [tv]. */
    val crew: List<CrewMember> = emptyList(),
    val tv: TvInfo? = null,
)

/** [episodeCount] is set for series: how many episodes this person appears in. */
data class CastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profilePath: String?,
    val episodeCount: Int? = null,
)

data class CrewMember(val id: Int, val name: String, val job: String, val profilePath: String?)

data class TvInfo(
    val seasonCount: Int,
    val episodeCount: Int,
    val firstAirYear: String?,
    /** Null while the series is still in production. */
    val lastAirYear: String?,
    val inProduction: Boolean,
    /** TMDB status, e.g. "Returning Series", "Ended", "Canceled". */
    val status: String?,
    val networks: List<Network>,
    val seasons: List<Season>,
    val creators: List<CrewMember>,
)

data class Season(
    val number: Int,
    val name: String,
    val episodeCount: Int,
    val airDate: String?,
    val posterPath: String?,
)

data class Network(val id: Int, val name: String, val logoPath: String?)

/** Someone in front of or behind the camera, with everything they've worked on. */
data class Person(
    val id: Int,
    val name: String,
    val biography: String,
    val birthday: LocalDate?,
    val deathday: LocalDate?,
    val placeOfBirth: String?,
    val profilePath: String?,
    /** "Acting", "Directing", "Writing", ... */
    val knownForDepartment: String?,
    val credits: List<PersonCredit>,
)

/**
 * One title in a filmography. [role] is the character for acting credits or the job(s) for crew
 * credits; [department] is "Acting" for cast and TMDB's crew department otherwise.
 */
data class PersonCredit(
    val title: Title,
    val role: String?,
    val department: String,
    val episodeCount: Int? = null,
)

/** A person as they appear in search results. */
data class PersonSummary(
    val id: Int,
    val name: String,
    val profilePath: String?,
    val knownForDepartment: String?,
    val knownFor: List<String>,
)

data class SearchResults(
    val titles: List<Title>,
    val people: List<PersonSummary>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

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

data class PagedTitles(val titles: List<Title>, val page: Int, val totalPages: Int) {
    val hasMore: Boolean get() = page < totalPages
}
