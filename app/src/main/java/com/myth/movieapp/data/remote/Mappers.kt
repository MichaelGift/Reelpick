package com.myth.movieapp.data.remote

import com.myth.movieapp.data.remote.dto.CreditDto
import com.myth.movieapp.data.remote.dto.GenreDto
import com.myth.movieapp.data.remote.dto.MovieDetailsDto
import com.myth.movieapp.data.remote.dto.MovieDto
import com.myth.movieapp.data.remote.dto.MultiResultDto
import com.myth.movieapp.data.remote.dto.PagedResponse
import com.myth.movieapp.data.remote.dto.PersonDto
import com.myth.movieapp.data.remote.dto.ProviderDto
import com.myth.movieapp.data.remote.dto.TvDetailsDto
import com.myth.movieapp.data.remote.dto.TvDto
import com.myth.movieapp.data.remote.dto.VideosDto
import com.myth.movieapp.data.remote.dto.WatchProvidersDto
import com.myth.movieapp.domain.model.CastMember
import com.myth.movieapp.domain.model.CrewMember
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Network
import com.myth.movieapp.domain.model.PagedTitles
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.PersonCredit
import com.myth.movieapp.domain.model.PersonSummary
import com.myth.movieapp.domain.model.SearchResults
import com.myth.movieapp.domain.model.Season
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.TvInfo
import com.myth.movieapp.domain.model.WatchProvider
import com.myth.movieapp.domain.model.WatchProviders
import java.time.LocalDate

private fun String?.nullIfBlank() = this?.takeIf { it.isNotBlank() }

private fun String?.toLocalDate(): LocalDate? = nullIfBlank()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun MovieDto.toDomain() = Title(
    id = id,
    title = title,
    mediaType = MediaType.MOVIE,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate.nullIfBlank(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    genreIds = genreIds,
)

fun TvDto.toDomain() = Title(
    id = id,
    title = name,
    mediaType = MediaType.TV,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = firstAirDate.nullIfBlank(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    popularity = popularity,
    genreIds = GenreMap.normalizeTv(genreIds),
)

fun GenreDto.toDomain() = Genre(id, name)

@JvmName("moviePage")
fun PagedResponse<MovieDto>.toPagedTitles() = PagedTitles(results.map { it.toDomain() }, page, totalPages)

@JvmName("tvPage")
fun PagedResponse<TvDto>.toPagedTitles() = PagedTitles(results.map { it.toDomain() }, page, totalPages)

private fun ProviderDto.toDomain() = WatchProvider(id, name, logoPath)

private fun VideosDto?.trailerKey(): String? = this?.results
    ?.filter { it.site.equals("YouTube", ignoreCase = true) && it.type == "Trailer" }
    ?.maxByOrNull { if (it.official) 1 else 0 }
    ?.key.nullIfBlank()

private fun WatchProvidersDto?.forRegion(region: String): WatchProviders? =
    this?.results?.get(region.uppercase())?.let { regional ->
        WatchProviders(
            link = regional.link,
            stream = regional.flatrate.map { it.toDomain() },
            rent = regional.rent.map { it.toDomain() },
            buy = regional.buy.map { it.toDomain() },
        )
    }?.takeUnless { it.isEmpty }

/** Crew worth surfacing on a film's page, in this order. */
private val featuredJobs = listOf("Director", "Screenplay", "Writer", "Story", "Novel")

fun MovieDetailsDto.toDomain(region: String): TitleDetails {
    val title = Title(
        id = id,
        title = title,
        mediaType = MediaType.MOVIE,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate.nullIfBlank(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = genres.map { it.id },
    )
    val crew = credits?.crew.orEmpty()
        .filter { it.job in featuredJobs }
        .sortedBy { featuredJobs.indexOf(it.job) }
        // One entry per person, listing all their featured jobs ("Director, Screenplay").
        .groupBy { it.id }
        .map { (_, jobs) ->
            val first = jobs.first()
            CrewMember(first.id, first.name, jobs.joinToString(", ") { it.job }, first.profilePath)
        }
        .take(6)

    return TitleDetails(
        title = title,
        tagline = tagline.nullIfBlank(),
        runtimeMinutes = runtime?.takeIf { it > 0 },
        genres = genres.map { it.toDomain() },
        cast = credits?.cast.orEmpty().sortedBy { it.order }.take(15).map {
            CastMember(it.id, it.name, it.character.nullIfBlank(), it.profilePath)
        },
        trailerYoutubeKey = videos.trailerKey(),
        watchProviders = watchProviders.forRegion(region),
        crew = crew,
    )
}

fun TvDetailsDto.toDomain(region: String): TitleDetails {
    val title = Title(
        id = id,
        title = name,
        mediaType = MediaType.TV,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = firstAirDate.nullIfBlank(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = GenreMap.normalizeTv(genres.map { it.id }),
    )
    val tv = TvInfo(
        seasonCount = numberOfSeasons,
        episodeCount = numberOfEpisodes,
        firstAirYear = firstAirDate.nullIfBlank()?.take(4),
        lastAirYear = lastAirDate.nullIfBlank()?.take(4)?.takeUnless { inProduction },
        inProduction = inProduction,
        status = status.nullIfBlank(),
        networks = networks.map { Network(it.id, it.name, it.logoPath) },
        // Season 0 is "Specials"; keep it, but after the numbered seasons.
        seasons = seasons
            .sortedWith(compareBy({ it.seasonNumber == 0 }, { it.seasonNumber }))
            .map { Season(it.seasonNumber, it.name, it.episodeCount, it.airDate.nullIfBlank(), it.posterPath) },
        creators = createdBy.map { CrewMember(it.id, it.name, "Creator", it.profilePath) },
    )
    return TitleDetails(
        title = title,
        tagline = tagline.nullIfBlank(),
        runtimeMinutes = episodeRunTime.firstOrNull { it > 0 },
        genres = genres.map { it.toDomain() },
        cast = aggregateCredits?.cast.orEmpty().sortedBy { it.order }.take(15).map { member ->
            CastMember(
                id = member.id,
                name = member.name,
                // The role they played most often.
                character = member.roles.maxByOrNull { it.episodeCount }?.character.nullIfBlank(),
                profilePath = member.profilePath,
                episodeCount = member.totalEpisodeCount.takeIf { it > 0 },
            )
        },
        trailerYoutubeKey = videos.trailerKey(),
        watchProviders = watchProviders.forRegion(region),
        tv = tv,
    )
}

/** A credit's title, or null for media types other than movie/tv. */
private fun CreditDto.toTitle(): Title? {
    val type = when (mediaType) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        else -> return null
    }
    return Title(
        id = id,
        title = (if (type == MediaType.MOVIE) title else name).orEmpty(),
        mediaType = type,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = (if (type == MediaType.MOVIE) releaseDate else firstAirDate).nullIfBlank(),
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genreIds = if (type == MediaType.TV) GenreMap.normalizeTv(genreIds) else genreIds,
    )
}

fun PersonDto.toDomain(): Person {
    val acting = combinedCredits?.cast.orEmpty().mapNotNull { credit ->
        credit.toTitle()?.let { PersonCredit(it, credit.character.nullIfBlank(), "Acting", credit.episodeCount) }
    }
    // TMDB lists a crew member once per job ("Writer", "Executive Producer", ...): merge them so each
    // title appears once per department.
    val crew = combinedCredits?.crew.orEmpty()
        .mapNotNull { credit -> credit.toTitle()?.let { it to credit } }
        .groupBy { (title, credit) -> title.key to (credit.department ?: "Crew") }
        .map { (key, entries) ->
            val (title, first) = entries.first()
            PersonCredit(
                title = title,
                role = entries.mapNotNull { it.second.job.nullIfBlank() }.distinct().joinToString(", ").nullIfBlank(),
                department = key.second,
                episodeCount = first.episodeCount,
            )
        }
    return Person(
        id = id,
        name = name,
        biography = biography.trim(),
        birthday = birthday.toLocalDate(),
        deathday = deathday.toLocalDate(),
        placeOfBirth = placeOfBirth.nullIfBlank(),
        profilePath = profilePath,
        knownForDepartment = knownForDepartment.nullIfBlank(),
        // Acting credits also dedupe: a guest star can be listed once per character.
        credits = acting.distinctBy { it.title.key } + crew,
    )
}

fun PagedResponse<MultiResultDto>.toSearchResults(): SearchResults {
    val titles = results.mapNotNull { hit ->
        when (hit.mediaType) {
            "movie" -> Title(
                id = hit.id, title = hit.title.orEmpty(), mediaType = MediaType.MOVIE,
                overview = hit.overview, posterPath = hit.posterPath, backdropPath = hit.backdropPath,
                releaseDate = hit.releaseDate.nullIfBlank(), voteAverage = hit.voteAverage,
                voteCount = hit.voteCount, popularity = hit.popularity, genreIds = hit.genreIds,
            )
            "tv" -> Title(
                id = hit.id, title = hit.name.orEmpty(), mediaType = MediaType.TV,
                overview = hit.overview, posterPath = hit.posterPath, backdropPath = hit.backdropPath,
                releaseDate = hit.firstAirDate.nullIfBlank(), voteAverage = hit.voteAverage,
                voteCount = hit.voteCount, popularity = hit.popularity,
                genreIds = GenreMap.normalizeTv(hit.genreIds),
            )
            else -> null
        }
    }
    val people = results.filter { it.mediaType == "person" }.map { hit ->
        PersonSummary(
            id = hit.id,
            name = hit.name.orEmpty(),
            profilePath = hit.profilePath,
            knownForDepartment = hit.knownForDepartment.nullIfBlank(),
            knownFor = hit.knownFor.mapNotNull { (it.title ?: it.name).nullIfBlank() }.take(3),
        )
    }
    return SearchResults(titles, people, page, totalPages)
}
