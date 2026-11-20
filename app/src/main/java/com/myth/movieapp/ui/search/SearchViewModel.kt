package com.myth.movieapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.PersonSummary
import com.myth.movieapp.domain.model.SearchResults
import com.myth.movieapp.domain.repository.TitleRepository
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    /** Set while browsing a genre instead of searching by text. */
    val browsing: Genre? = null,
    val results: List<Title> = emptyList(),
    /** People matching a text search (first page only); empty while browsing a genre. */
    val people: List<PersonSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    /** True once a search for the current query has completed, so "no results" can be shown. */
    val searched: Boolean = false,
) {
    val isIdle: Boolean get() = query.isBlank() && browsing == null
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movies: TitleRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = preferences.preferences
        .map { it.recentSearches }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    /** Genres for the "browse" tiles shown before the user types anything. */
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

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
        viewModelScope.launch { movies.genres().onSuccess { _genres.value = it } }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, browsing = null) }
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
        _state.update { it.copy(query = query, browsing = null) }
        queries.value = query
        search(trimmed)
        viewModelScope.launch { preferences.addRecentSearch(trimmed) }
    }

    fun browse(genre: Genre) {
        searchJob?.cancel()
        lastSearched = ""
        queries.value = ""
        _state.value = SearchUiState(browsing = genre, isLoading = true)
        searchJob = viewModelScope.launch {
            load(1).onSuccess { result ->
                page = 1
                _state.update { it.copy(results = result.titles, canLoadMore = result.hasMore, isLoading = false, searched = true) }
            }.onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun clearBrowse() = onQueryChange("")

    fun clearRecentSearches() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore || current.isLoadingMore || current.isLoading) return
        _state.update { it.copy(isLoadingMore = true) }
        searchJob = viewModelScope.launch {
            load(page + 1)
                .onSuccess { result ->
                    page = result.page
                    _state.update {
                        it.copy(
                            results = (it.results + result.titles).distinctBy(Title::key),
                            canLoadMore = result.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.toUserMessage()) } }
        }
    }

    /** One page of either the text search or the genre being browsed. */
    private suspend fun load(page: Int): Result<SearchResults> {
        val browsing = _state.value.browsing ?: return movies.search(_state.value.query.trim(), page)
        // Browsing mixes films and series; it only fails if both fail.
        val (films, series) = coroutineScope {
            val films = async { movies.discoverByGenre(MediaType.MOVIE, browsing.id, page) }
            val series = async { movies.discoverByGenre(MediaType.TV, browsing.id, page) }
            films.await() to series.await()
        }
        if (films.isFailure && series.isFailure) return Result.failure(films.exceptionOrNull()!!)
        val titles = interleave(films.getOrDefault(emptyList()), series.getOrDefault(emptyList()))
        return Result.success(SearchResults(titles, emptyList(), page, if (titles.isEmpty()) page else MAX_BROWSE_PAGES))
    }

    private fun <T> interleave(a: List<T>, b: List<T>): List<T> =
        (0 until maxOf(a.size, b.size)).flatMap { i -> listOfNotNull(a.getOrNull(i), b.getOrNull(i)) }

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
                            results = result.titles,
                            people = result.people,
                            canLoadMore = result.hasMore,
                            isLoading = false,
                            searched = true,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    private companion object {
        const val MAX_BROWSE_PAGES = 10
    }
}
