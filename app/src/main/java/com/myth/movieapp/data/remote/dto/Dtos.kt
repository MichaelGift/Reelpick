package com.myth.movieapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class MovieDto(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class GenreDto(val id: Int, val name: String)

@Serializable
data class GenresResponse(val genres: List<GenreDto> = emptyList())

@Serializable
data class MovieDetailsDto(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    val tagline: String? = null,
    val runtime: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val genres: List<GenreDto> = emptyList(),
    val credits: CreditsDto? = null,
    val videos: VideosDto? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto? = null,
)

@Serializable
data class CreditsDto(
    val cast: List<CastDto> = emptyList(),
    val crew: List<CrewDto> = emptyList(),
)

@Serializable
data class CrewDto(
    val id: Int,
    val name: String = "",
    val job: String = "",
    val department: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class CastDto(
    val id: Int,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)

@Serializable
data class VideosDto(val results: List<VideoDto> = emptyList())

@Serializable
data class VideoDto(
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)

@Serializable
data class WatchProvidersDto(val results: Map<String, RegionProvidersDto> = emptyMap())

@Serializable
data class RegionProvidersDto(
    val link: String? = null,
    val flatrate: List<ProviderDto> = emptyList(),
    val rent: List<ProviderDto> = emptyList(),
    val buy: List<ProviderDto> = emptyList(),
)

@Serializable
data class ProviderDto(
    @SerialName("provider_id") val id: Int,
    @SerialName("provider_name") val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
)

// ---- TV ----

@Serializable
data class TvDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class TvDetailsDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    val status: String? = null,
    @SerialName("in_production") val inProduction: Boolean = false,
    val networks: List<NetworkDto> = emptyList(),
    val seasons: List<SeasonDto> = emptyList(),
    @SerialName("created_by") val createdBy: List<CreatorDto> = emptyList(),
    @SerialName("aggregate_credits") val aggregateCredits: AggregateCreditsDto? = null,
    val videos: VideosDto? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto? = null,
)

@Serializable
data class NetworkDto(
    val id: Int,
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
)

@Serializable
data class SeasonDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
data class CreatorDto(
    val id: Int,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)

/** Cast across every season, with how many episodes each person appears in. */
@Serializable
data class AggregateCreditsDto(val cast: List<AggregateCastDto> = emptyList())

@Serializable
data class AggregateCastDto(
    val id: Int,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val roles: List<RoleDto> = emptyList(),
    @SerialName("total_episode_count") val totalEpisodeCount: Int = 0,
    val order: Int = Int.MAX_VALUE,
)

@Serializable
data class RoleDto(
    val character: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
)

// ---- People ----

@Serializable
data class PersonDto(
    val id: Int,
    val name: String = "",
    val biography: String = "",
    val birthday: String? = null,
    val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("combined_credits") val combinedCredits: CombinedCreditsDto? = null,
)

@Serializable
data class CombinedCreditsDto(
    val cast: List<CreditDto> = emptyList(),
    val crew: List<CreditDto> = emptyList(),
)

/** A filmography entry: a movie or TV credit, as cast (character) or crew (job). */
@Serializable
data class CreditDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String = "",
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val character: String? = null,
    val job: String? = null,
    val department: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

// ---- Multi-search ----

/** One `search/multi` hit: a movie, a series or a person, told apart by [mediaType]. */
@Serializable
data class MultiResultDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String = "",
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("known_for") val knownFor: List<MultiResultDto> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)
