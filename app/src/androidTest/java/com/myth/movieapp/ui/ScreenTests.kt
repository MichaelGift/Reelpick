package com.myth.movieapp.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.recommend.CandidateSource
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.recommend.RecommendationRanker
import com.myth.movieapp.testing.FakeFeedCacheRepository
import com.myth.movieapp.testing.FakeFeedbackRepository
import com.myth.movieapp.testing.FakeTitleRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.FakeWatchlistRepository
import com.myth.movieapp.testing.movie
import com.myth.movieapp.testing.movieKey
import com.myth.movieapp.testing.tasteProfileUseCase
import com.myth.movieapp.ui.details.DetailsScreen
import com.myth.movieapp.ui.details.DetailsViewModel
import com.myth.movieapp.domain.model.CastMember
import com.myth.movieapp.domain.model.CrewMember
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Network
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.PersonCredit
import com.myth.movieapp.domain.model.Season
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TvInfo
import com.myth.movieapp.testing.series
import com.myth.movieapp.testing.seriesKey
import com.myth.movieapp.ui.person.PersonScreen
import com.myth.movieapp.ui.person.PersonViewModel
import androidx.compose.ui.test.performScrollTo
import java.time.LocalDate
import com.myth.movieapp.ui.onboarding.OnboardingScreen
import com.myth.movieapp.ui.onboarding.OnboardingViewModel
import com.myth.movieapp.ui.theme.MovieAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class ScreenTests {

    @get:Rule val compose = createComposeRule()

    private val movies = FakeTitleRepository()
    private val feedback = FakeFeedbackRepository()
    private val watchlist = FakeWatchlistRepository()
    private val prefs = FakeUserPreferencesRepository()

    @Test
    fun onboardingPicksGenresAndSeedsThenCompletes() {
        val genres = listOf("Action", "Comedy", "Drama", "Horror").mapIndexed { i, name -> Genre(i + 1, name) }
        movies.genres = genres
        genres.forEach { g ->
            movies.discover[g.id] = Result.success(
                (1..3).map { movie(g.id * 10 + it, listOf(g.id), title = "${g.name} $it").copy(posterPath = "/x.jpg") },
            )
        }
        val useCase = GetForYouFeedUseCase(
            movies, tasteProfileUseCase(feedback, watchlist, prefs), FakeFeedCacheRepository(), CandidateSource(movies),
            RecommendationRanker(), Clock.systemUTC(),
        )
        val viewModel = OnboardingViewModel(movies, feedback, prefs, useCase)
        compose.setContent { MovieAppTheme { OnboardingScreen(viewModel) } }

        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithText("Pick 3 more").assertIsNotEnabled()
        listOf("Action", "Comedy", "Drama").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Continue").assertIsEnabled().performClick()

        compose.waitUntil(5_000) { viewModel.state.value.movies.isNotEmpty() }
        listOf("Action 1", "Comedy 1", "Drama 1", "Action 2", "Comedy 2").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        compose.onNodeWithText("Build my feed").assertIsEnabled().performClick()

        compose.waitUntil(5_000) { runBlocking { prefs.preferences.first().onboardingComplete } }
        assertEquals(5, runBlocking { feedback.feedback.first().size })
    }

    @Test
    fun detailsWatchlistToggleUpdatesRepository() {
        val dune = movie(42, title = "Dune")
        movies.details[movieKey(42)] = Result.success(
            TitleDetails(dune, tagline = null, runtimeMinutes = 155, genres = emptyList(), cast = emptyList(),
                trailerYoutubeKey = null, watchProviders = null),
        )
        val viewModel = DetailsViewModel(
            SavedStateHandle(mapOf("titleId" to 42, "title" to "Dune")), movies, feedback, watchlist, prefs,
            tasteProfileUseCase(feedback, watchlist, prefs),
        )
        compose.setContent { MovieAppTheme { DetailsScreen(onBack = {}, onMovieClick = { _, _ -> }, onPersonClick = { _, _, _, _ -> }, viewModel = viewModel) } }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("2h 35m", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Dune").onFirst().assertExists()
        compose.onNodeWithText("Add to watchlist").performClick()

        compose.waitUntil(5_000) { runBlocking { watchlist.isInWatchlist(movieKey(42)).first() } }
        compose.onNodeWithText("On your watchlist").assertExists()
        assertTrue(runBlocking { watchlist.watchlist.first() }.any { it.id == 42 })
    }

    @Test
    fun seriesDetailsShowSeasonsAndEpisodeCounts() {
        val dark = series(70523, title = "Dark")
        movies.details[seriesKey(70523)] = Result.success(
            TitleDetails(
                dark, tagline = null, runtimeMinutes = 60, genres = emptyList(),
                cast = listOf(CastMember(1, "Louis Hofmann", "Jonas Kahnwald", null, episodeCount = 26)),
                trailerYoutubeKey = null, watchProviders = null,
                tv = TvInfo(
                    seasonCount = 3, episodeCount = 26, firstAirYear = "2017", lastAirYear = "2020", inProduction = false,
                    status = "Ended", networks = listOf(Network(213, "Netflix", null)),
                    seasons = listOf(Season(1, "Season 1", 10, "2017-12-01", null), Season(2, "Season 2", 8, "2019-06-21", null)),
                    creators = listOf(CrewMember(9, "Baran bo Odar", "Creator", null)),
                ),
            ),
        )
        val viewModel = DetailsViewModel(
            SavedStateHandle(mapOf("titleId" to 70523, "mediaType" to MediaType.TV, "title" to "Dark")),
            movies, feedback, watchlist, prefs, tasteProfileUseCase(feedback, watchlist, prefs),
        )
        compose.setContent {
            MovieAppTheme { DetailsScreen(onBack = {}, onMovieClick = { _, _ -> }, onPersonClick = { _, _, _, _ -> }, viewModel = viewModel) }
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("2017–2020", substring = true).fetchSemanticsNodes().isNotEmpty() }
        // Shown in the header meta line and again as the Seasons subtitle.
        assertEquals(2, compose.onAllNodesWithText("3 seasons", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Ended").assertExists()
        compose.onNodeWithText("Created by").performScrollTo().assertExists()
        compose.onNodeWithText("Season 2").performScrollTo().assertExists()
        compose.onNodeWithText("26 episodes").performScrollTo().assertExists()
    }

    @Test
    fun personPageListsFilmographyAndOpensACredit() {
        val inception = movie(27205, title = "Inception", releaseDate = "2010-07-15")
        val dark = series(70523, title = "Dark", releaseDate = "2017-12-01")
        movies.people[525] = Result.success(
            Person(
                id = 525, name = "Christopher Nolan", biography = "British-American filmmaker.",
                birthday = LocalDate.of(1970, 7, 30), deathday = null, placeOfBirth = "London",
                profilePath = null, knownForDepartment = "Directing",
                credits = listOf(
                    PersonCredit(inception, "Director", "Directing"),
                    PersonCredit(dark, "Himself", "Acting", episodeCount = 1),
                ),
            ),
        )
        val viewModel = PersonViewModel(
            SavedStateHandle(mapOf("personId" to 525, "name" to "Christopher Nolan")),
            movies, tasteProfileUseCase(feedback, watchlist, prefs), Clock.systemUTC(),
        )
        var opened: Title? = null
        compose.setContent { MovieAppTheme { PersonScreen(onBack = {}, onTitleClick = { title, _ -> opened = title }, viewModel = viewModel) } }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("DIRECTOR").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Born", substring = true).assertExists()
        compose.onNodeWithText("Filmography").performScrollTo().assertExists()
        // Directing leads; switch to Acting to find the series appearance.
        compose.onNodeWithText("Acting").performScrollTo().performClick()
        compose.onNodeWithText("Himself  ·  1 ep").performScrollTo().performClick()

        assertEquals(dark, opened)
    }
}

