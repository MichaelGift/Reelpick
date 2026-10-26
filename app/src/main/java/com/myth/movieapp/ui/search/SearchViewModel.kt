package com.myth.movieapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.repository.MovieRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    /** True once a search for the current query has completed, so "no results" can be shown. */
    val searched: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movies: MovieRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = preferences.preferences
        .map { it.recentSearches }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val queries = MutableStateFlow("")
    private var page = 1
    private var searchJob: Job? = null
    /** Last query actually sent, so a submit followed by the debounce doesn't search twice. */
    private var lastSearched = ""

    init {
        queries
            .map { it.trim() }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { if (it != lastSearched) search(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queries.value = query
        if (query.isBlank()) {
            searchJob?.cancel()
            lastSearched = ""
            _state.update { SearchUiState(query = query) }
        }
    }

    /** Called on IME search or tapping a recent search: runs immediately and remembers the query. */
    fun onSubmit(query: String = _state.value.query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _state.update { it.copy(query = query) }
        queries.value = query
        search(trimmed)
        viewModelScope.launch { preferences.addRecentSearch(trimmed) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore || current.isLoadingMore || current.isLoading) return
        _state.update { it.copy(isLoadingMore = true) }
        searchJob = viewModelScope.launch {
            movies.search(current.query.trim(), page + 1)
                .onSuccess { result ->
                    page = result.page
                    _state.update {
                        it.copy(
                            results = (it.results + result.movies).distinctBy(Movie::id),
                            canLoadMore = result.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.toUserMessage()) } }
        }
    }

    private fun search(query: String) {
        if (query.isEmpty()) return
        searchJob?.cancel()
        lastSearched = query
        _state.update { it.copy(isLoading = true, error = null) }
        searchJob = viewModelScope.launch {
            movies.search(query, 1)
                .onSuccess { result ->
                    page = 1
                    _state.update {
                        it.copy(
                            results = result.movies,
                            canLoadMore = result.hasMore,
                            isLoading = false,
                            searched = true,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }
}
