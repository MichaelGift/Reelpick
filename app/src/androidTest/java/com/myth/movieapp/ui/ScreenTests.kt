package com.myth.movieapp.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.recommend.CandidateSource
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.recommend.RecommendationRanker
import com.myth.movieapp.testing.FakeFeedCacheRepository
import com.myth.movieapp.testing.FakeFeedbackRepository
import com.myth.movieapp.testing.FakeMovieRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.FakeWatchlistRepository
import com.myth.movieapp.testing.movie
import com.myth.movieapp.ui.details.DetailsScreen
import com.myth.movieapp.ui.details.DetailsViewModel
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

    private val movies = FakeMovieRepository()
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
            movies, feedback, watchlist, prefs, FakeFeedCacheRepository(), CandidateSource(movies),
            RecommendationRanker(), Clock.systemUTC(),
        )
        val viewModel = OnboardingViewModel(movies, feedback, prefs, useCase)
        compose.setContent { MovieAppTheme { OnboardingScreen(viewModel) } }

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
        movies.details[42] = Result.success(
            MovieDetails(dune, tagline = null, runtimeMinutes = 155, genres = emptyList(), cast = emptyList(),
                trailerYoutubeKey = null, watchProviders = null),
        )
        val viewModel = DetailsViewModel(SavedStateHandle(mapOf("movieId" to 42)), movies, feedback, watchlist, prefs)
        compose.setContent { MovieAppTheme { DetailsScreen(onBack = {}, onMovieClick = {}, viewModel = viewModel) } }

        compose.onNodeWithText("Dune").assertExists()
        compose.onNodeWithText("2h 35m", substring = true).assertExists()
        compose.onNodeWithContentDescription("Watchlist").performClick()

        compose.waitUntil(5_000) { runBlocking { watchlist.isInWatchlist(42).first() } }
        assertTrue(runBlocking { watchlist.watchlist.first() }.any { it.id == 42 })
    }
}
