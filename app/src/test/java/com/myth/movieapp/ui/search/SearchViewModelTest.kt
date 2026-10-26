package com.myth.movieapp.ui.search

import app.cash.turbine.test
import com.myth.movieapp.domain.model.PagedMovies
import com.myth.movieapp.testing.FakeMovieRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.MainDispatcherRule
import com.myth.movieapp.testing.movie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val movies = FakeMovieRepository().apply {
        searchResults = { query, page -> PagedMovies(listOf(movie(page, title = "$query $page")), page, 2) }
    }
    private val prefs = FakeUserPreferencesRepository()

    @Test
    fun `typing is debounced into a single search`() = runTest(main.dispatcher) {
        val viewModel = SearchViewModel(movies, prefs)

        viewModel.onQueryChange("d")
        advanceTimeBy(100)
        viewModel.onQueryChange("du")
        advanceTimeBy(100)
        viewModel.onQueryChange("dune")
        advanceUntilIdle()

        assertEquals(listOf("dune"), movies.searchQueries)
        assertEquals(listOf("dune 1"), viewModel.state.value.results.map { it.title })
        assertTrue(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `load more appends the next page and stops at the last one`() = runTest(main.dispatcher) {
        val viewModel = SearchViewModel(movies, prefs)
        viewModel.onSubmit("alien")
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("alien 1", "alien 2"), viewModel.state.value.results.map { it.title })
        assertFalse(viewModel.state.value.canLoadMore)
        // The submit ran immediately; the debounce must not repeat it and reset to page 1.
        assertEquals(listOf("alien", "alien"), movies.searchQueries)
    }

    @Test
    fun `submitting remembers the query and clearing the field resets results`() = runTest(main.dispatcher) {
        val viewModel = SearchViewModel(movies, prefs)

        viewModel.recentSearches.test {
            assertEquals(emptyList<String>(), awaitItem())
            viewModel.onSubmit("  heat ")
            runCurrent()
            assertEquals(listOf("heat"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onQueryChange("")
        advanceUntilIdle()
        assertEquals(SearchUiState(query = ""), viewModel.state.value)
    }
}
