package com.myth.playpick.ui

import androidx.lifecycle.SavedStateHandle
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.UserPreferences
import com.myth.playpick.domain.recommend.GameTaste
import com.myth.playpick.domain.recommend.GetTasteProfileUseCase
import com.myth.playpick.testing.FakeFeedbackRepository
import com.myth.playpick.testing.FakeGameRepository
import com.myth.playpick.testing.FakeLibraryRepository
import com.myth.playpick.testing.FakePreferencesRepository
import com.myth.playpick.testing.MainDispatcherRule
import com.myth.playpick.testing.game
import com.myth.playpick.ui.details.DetailsContent
import com.myth.playpick.ui.details.GameDetailsViewModel
import com.myth.playpick.ui.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelsTest {

    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    private val rpg = 5
    private val rawg = FakeGameRepository()
    private val prefs = FakePreferencesRepository(UserPreferences(onboardingComplete = true, platforms = setOf(Platform.PC), genreIds = setOf(rpg)))
    private val feedback = FakeFeedbackRepository()
    private val library = FakeLibraryRepository()

    private fun details(id: Int) = GameDetailsViewModel(
        SavedStateHandle(mapOf("gameId" to id, "name" to "Game $id")),
        rawg, feedback, library, prefs,
        GetTasteProfileUseCase(feedback, library, prefs),
    )

    @Test
    fun `a game's own rating doesn't inflate its own match`() {
        val profile = GameTaste.build(setOf(rpg), emptySet(), listOf(Feedback(game(9, genres = listOf(35)), FeedbackType.LIKE, 1)), emptyList())

        val fit = GameDetailsViewModel.fit(game(1, genres = listOf(rpg)), profile)

        assertTrue(fit!!.match > 0)
        assertEquals(listOf("Genre $rpg"), fit.reasons)
        assertNull(GameDetailsViewModel.fit(game(2, genres = listOf(15)), profile))
    }

    @Test
    fun `details load extras and the rarest achievements`() = runTest {
        rawg.achievementsById[1] = listOf(Achievement(1, "Easy", "", percent = 80.0), Achievement(2, "Hard", "", percent = 1.2), Achievement(3, "Mid", "", percent = 12.0))
        rawg.seriesById[1] = listOf(game(2), game(1))

        val vm = details(1)
        advanceUntilIdle()

        val loaded = vm.content.value as DetailsContent.Loaded
        assertEquals(listOf("Hard", "Mid", "Easy"), loaded.rarest.map { it.name })
        assertEquals(listOf(2), loaded.series.map { it.id })
    }

    @Test
    fun `rating toggles and status changes reach the repositories`() = runTest {
        val vm = details(1)
        advanceUntilIdle()

        vm.rate(FeedbackType.LIKE); advanceUntilIdle()
        assertEquals(FeedbackType.LIKE, feedback.feedbackFor(1).first())
        vm.rate(FeedbackType.LIKE); advanceUntilIdle()
        assertNull(feedback.feedbackFor(1).first())

        vm.setStatus(LibraryStatus.PLAYING); advanceUntilIdle()
        assertEquals(LibraryStatus.PLAYING, library.entry(1).first()!!.status)
        vm.setStatus(null); advanceUntilIdle()
        assertNull(library.entry(1).first())
    }

    @Test
    fun `search waits for typing to pause, then searches everywhere`() = runTest {
        rawg.whenQuery({ it.search == "hollow" }, listOf(game(9767), game(1)))
        val vm = SearchViewModel(rawg, prefs)
        advanceUntilIdle()

        vm.onQueryChange("hol"); advanceTimeBy(100)
        vm.onQueryChange("hollow"); advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 50); advanceUntilIdle()

        assertEquals(listOf(9767, 1), vm.state.value.results.map { it.id })
        assertEquals(1, rawg.queries.count { it.search != null })
        assertTrue(rawg.queries.single { it.search != null }.platforms.isEmpty())
    }

    @Test
    fun `browsing a genre stays on the user's platforms`() = runTest {
        rawg.whenQuery({ rpg in it.genreIds }, listOf(game(3)))
        val vm = SearchViewModel(rawg, prefs)
        advanceUntilIdle()

        vm.browse(Named(rpg, "RPG")); advanceUntilIdle()

        assertEquals(listOf(3), vm.state.value.results.map { it.id })
        assertEquals(setOf(Platform.PC), rawg.queries.last().platforms)
    }

    @Test
    fun `submitting remembers the search`() = runTest {
        val vm = SearchViewModel(rawg, prefs)
        vm.onQueryChange("zelda")
        vm.submit(); advanceUntilIdle()

        assertEquals(listOf("zelda"), prefs.state.value.recentSearches)
    }
}
