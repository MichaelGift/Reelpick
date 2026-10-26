package com.myth.movieapp.ui.onboarding

import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.recommend.CandidateSource
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.recommend.RecommendationRanker
import com.myth.movieapp.testing.FakeFeedCacheRepository
import com.myth.movieapp.testing.FakeFeedbackRepository
import com.myth.movieapp.testing.FakeMovieRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.FakeWatchlistRepository
import com.myth.movieapp.testing.MainDispatcherRule
import com.myth.movieapp.testing.movie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val genres = listOf(Genre(1, "Action"), Genre(2, "Comedy"), Genre(3, "Drama"), Genre(4, "Horror"))
    private val movies = FakeMovieRepository().apply {
        this.genres = this@OnboardingViewModelTest.genres
        genres.forEach { g ->
            discover[g.id] = Result.success((1..4).map { i -> movie(g.id * 10 + i, listOf(g.id)).copy(posterPath = "/p.jpg") })
        }
    }
    private val feedback = FakeFeedbackRepository()
    private val prefs = FakeUserPreferencesRepository()
    private val watchlist = FakeWatchlistRepository()
    private val cache = FakeFeedCacheRepository()

    private fun viewModel() = OnboardingViewModel(
        movies,
        feedback,
        prefs,
        GetForYouFeedUseCase(
            movies, feedback, watchlist, prefs, cache, CandidateSource(movies), RecommendationRanker(), Clock.systemUTC(),
        ),
    )

    @Test
    fun `requires three genres then interleaves movies from each`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(genres, vm.state.value.genres)

        vm.toggleGenre(1)
        vm.toggleGenre(2)
        assertFalse(vm.state.value.canContinue)
        vm.toggleGenre(3)
        assertTrue(vm.state.value.canContinue)

        vm.continueToMovies()
        advanceUntilIdle()

        val firstThree = vm.state.value.movies.take(3).map { it.genreIds.single() }
        assertEquals(listOf(1, 2, 3), firstThree)
    }

    @Test
    fun `finishing saves each liked movie as a seed and completes onboarding`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        listOf(1, 2, 3).forEach(vm::toggleGenre)
        vm.continueToMovies()
        advanceUntilIdle()

        val picks = vm.state.value.movies.take(5).map { it.id }
        picks.take(4).forEach(vm::toggleLike)
        assertFalse(vm.state.value.canFinish)
        vm.toggleLike(picks[4])
        vm.finish()
        advanceUntilIdle()

        val saved = feedback.feedback.first()
        assertEquals(picks.toSet(), saved.map { it.movie.id }.toSet())
        assertTrue(saved.all { it.type == FeedbackType.LIKE })
        val p = prefs.preferences.first()
        assertTrue(p.onboardingComplete)
        assertEquals(setOf(1, 2, 3), p.selectedGenreIds)
    }
}
