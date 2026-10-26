package com.myth.movieapp.data.remote

import com.myth.movieapp.data.remote.dto.GenreDto
import com.myth.movieapp.data.remote.dto.MovieDetailsDto
import com.myth.movieapp.data.remote.dto.MovieDto
import com.myth.movieapp.data.remote.dto.PagedResponse
import com.myth.movieapp.data.remote.dto.ProviderDto
import com.myth.movieapp.domain.model.CastMember
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.PagedMovies
import com.myth.movieapp.domain.model.WatchProvider
import com.myth.movieapp.domain.model.WatchProviders

fun MovieDto.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    genreIds = genreIds,
)

fun GenreDto.toDomain() = Genre(id, name)

fun PagedResponse<MovieDto>.toPagedMovies() = PagedMovies(
    movies = results.map { it.toDomain() },
    page = page,
    totalPages = totalPages,
)

private fun ProviderDto.toDomain() = WatchProvider(id, name, logoPath)

fun MovieDetailsDto.toDomain(region: String): MovieDetails {
    val movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate?.takeIf { it.isNotBlank() },
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = genres.map { it.id },
    )
    val trailer = videos?.results
        ?.filter { it.site.equals("YouTube", ignoreCase = true) && it.type == "Trailer" }
        ?.maxByOrNull { if (it.official) 1 else 0 }
    val providers = watchProviders?.results?.get(region.uppercase())?.let { regional ->
        WatchProviders(
            link = regional.link,
            stream = regional.flatrate.map { it.toDomain() },
            rent = regional.rent.map { it.toDomain() },
            buy = regional.buy.map { it.toDomain() },
        )
    }?.takeUnless { it.isEmpty }

    return MovieDetails(
        movie = movie,
        tagline = tagline?.takeIf { it.isNotBlank() },
        runtimeMinutes = runtime?.takeIf { it > 0 },
        genres = genres.map { it.toDomain() },
        cast = credits?.cast.orEmpty().sortedBy { it.order }.take(15).map {
            CastMember(it.id, it.name, it.character?.takeIf(String::isNotBlank), it.profilePath)
        },
        trailerYoutubeKey = trailer?.key?.takeIf { it.isNotBlank() },
        watchProviders = providers,
    )
}
