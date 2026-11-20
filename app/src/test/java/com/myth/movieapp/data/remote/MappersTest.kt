package com.myth.movieapp.data.remote

import com.myth.movieapp.data.remote.dto.MovieDetailsDto
import com.myth.movieapp.data.remote.dto.MultiResultDto
import com.myth.movieapp.data.remote.dto.PagedResponse
import com.myth.movieapp.data.remote.dto.PersonDto
import com.myth.movieapp.data.remote.dto.TvDetailsDto
import com.myth.movieapp.domain.model.MediaType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Parses trimmed-down TMDB payloads with the app's Json settings and checks the domain mapping. */
class MappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `series details map seasons, cast episodes, creators and normalized genres`() {
        val dto = json.decodeFromString<TvDetailsDto>(
            """
            {
              "id": 1399, "name": "Game of Thrones", "first_air_date": "2011-04-17", "last_air_date": "2019-05-19",
              "in_production": false, "status": "Ended", "number_of_seasons": 8, "number_of_episodes": 73,
              "episode_run_time": [60], "vote_average": 8.4, "vote_count": 25000,
              "genres": [{"id": 10765, "name": "Sci-Fi & Fantasy"}, {"id": 18, "name": "Drama"}],
              "networks": [{"id": 49, "name": "HBO", "logo_path": "/hbo.png"}],
              "created_by": [{"id": 9813, "name": "David Benioff", "profile_path": null}],
              "seasons": [
                {"season_number": 0, "name": "Specials", "episode_count": 14},
                {"season_number": 2, "name": "Season 2", "episode_count": 10, "air_date": "2012-04-01"},
                {"season_number": 1, "name": "Season 1", "episode_count": 10, "air_date": "2011-04-17"}
              ],
              "aggregate_credits": {"cast": [
                {"id": 2, "name": "Emilia Clarke", "order": 1, "total_episode_count": 62,
                 "roles": [{"character": "Daenerys Targaryen", "episode_count": 62}]},
                {"id": 1, "name": "Kit Harington", "order": 0, "total_episode_count": 67,
                 "roles": [{"character": "Jon Snow", "episode_count": 62}, {"character": "Aegon", "episode_count": 5}]}
              ]},
              "watch/providers": {"results": {"US": {"flatrate": [{"provider_id": 1899, "provider_name": "Max"}]}}}
            }
            """,
        )

        val details = dto.toDomain(region = "us")

        assertEquals(MediaType.TV, details.title.mediaType)
        assertEquals("Game of Thrones", details.title.title)
        assertEquals("2011-04-17", details.title.releaseDate)
        assertEquals(listOf(878, 14, 18), details.title.genreIds)
        assertEquals(60, details.runtimeMinutes)
        assertEquals(listOf("Kit Harington", "Emilia Clarke"), details.cast.map { it.name })
        assertEquals("Jon Snow", details.cast.first().character)
        assertEquals(67, details.cast.first().episodeCount)
        assertEquals("Max", details.watchProviders!!.stream.single().name)

        val tv = details.tv!!
        assertEquals(8, tv.seasonCount)
        assertEquals(73, tv.episodeCount)
        assertEquals("2011", tv.firstAirYear)
        assertEquals("2019", tv.lastAirYear)
        assertEquals(listOf(1, 2, 0), tv.seasons.map { it.number }) // specials last
        assertEquals("David Benioff", tv.creators.single().name)
        assertEquals("HBO", tv.networks.single().name)
    }

    @Test
    fun `a running series has no end year`() {
        val dto = json.decodeFromString<TvDetailsDto>(
            """{"id": 1, "name": "X", "first_air_date": "2020-01-01", "last_air_date": "2025-06-01", "in_production": true}""",
        )
        assertNull(dto.toDomain("US").tv!!.lastAirYear)
    }

    @Test
    fun `film crew keeps directors first and merges a person's jobs`() {
        val dto = json.decodeFromString<MovieDetailsDto>(
            """
            {"id": 27205, "title": "Inception", "credits": {"cast": [], "crew": [
              {"id": 525, "name": "Christopher Nolan", "job": "Screenplay"},
              {"id": 1, "name": "Someone", "job": "Producer"},
              {"id": 525, "name": "Christopher Nolan", "job": "Director"}
            ]}}
            """,
        )

        val crew = dto.toDomain("US").crew

        assertEquals(1, crew.size)
        assertEquals("Christopher Nolan", crew.single().name)
        assertEquals("Director, Screenplay", crew.single().job)
    }

    @Test
    fun `person credits merge crew jobs per title and skip unknown media`() {
        val dto = json.decodeFromString<PersonDto>(
            """
            {"id": 525, "name": "Christopher Nolan", "birthday": "1970-07-30", "deathday": null,
             "biography": "  Director.  ", "known_for_department": "Directing",
             "combined_credits": {
               "cast": [
                 {"id": 9, "media_type": "movie", "title": "Cameo", "character": "Himself"},
                 {"id": 9, "media_type": "movie", "title": "Cameo", "character": "Voice"}
               ],
               "crew": [
                 {"id": 27205, "media_type": "movie", "title": "Inception", "job": "Director", "department": "Directing"},
                 {"id": 27205, "media_type": "movie", "title": "Inception", "job": "Writer", "department": "Writing"},
                 {"id": 27205, "media_type": "movie", "title": "Inception", "job": "Screenplay", "department": "Writing"},
                 {"id": 27205, "media_type": "tv", "name": "Inception: The Series", "job": "Director", "department": "Directing",
                  "genre_ids": [10759]},
                 {"id": 5, "media_type": "person", "name": "Not a title"}
               ]
             }}
            """,
        )

        val person = dto.toDomain()

        assertEquals(LocalDate.of(1970, 7, 30), person.birthday)
        assertNull(person.deathday)
        assertEquals("Director.", person.biography)
        val acting = person.credits.filter { it.department == "Acting" }
        assertEquals(1, acting.size) // two characters in one film collapse to one credit
        val writing = person.credits.single { it.department == "Writing" }
        assertEquals("Writer, Screenplay", writing.role)
        // Same numeric id, different media type: two separate directing credits.
        val directing = person.credits.filter { it.department == "Directing" }
        assertEquals(setOf(MediaType.MOVIE, MediaType.TV), directing.map { it.title.mediaType }.toSet())
        assertEquals(listOf(28, 12), directing.single { it.title.mediaType == MediaType.TV }.title.genreIds)
    }

    @Test
    fun `multi-search splits titles from people and keeps relevance order`() {
        val dto = json.decodeFromString<PagedResponse<MultiResultDto>>(
            """
            {"page": 1, "total_pages": 3, "results": [
              {"id": 1, "media_type": "tv", "name": "Dark", "first_air_date": "2017-12-01", "genre_ids": [10765]},
              {"id": 525, "media_type": "person", "name": "Christopher Nolan", "known_for_department": "Directing",
               "known_for": [{"id": 2, "media_type": "movie", "title": "Inception"}, {"id": 3, "media_type": "tv", "name": "Show"}]},
              {"id": 2, "media_type": "movie", "title": "The Dark Knight", "release_date": "2008-07-16"}
            ]}
            """,
        )

        val results = dto.toSearchResults()

        assertEquals(listOf("Dark", "The Dark Knight"), results.titles.map { it.title })
        assertEquals(listOf(MediaType.TV, MediaType.MOVIE), results.titles.map { it.mediaType })
        assertEquals(listOf(878, 14), results.titles.first().genreIds)
        assertEquals("Christopher Nolan", results.people.single().name)
        assertEquals(listOf("Inception", "Show"), results.people.single().knownFor)
        assertTrue(results.hasMore)
    }
}
