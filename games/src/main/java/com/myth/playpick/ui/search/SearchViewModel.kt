package com.myth.playpick.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import com.myth.playpick.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    /** Set while browsing a genre instead of searching. */
    val browsing: Named? = null,
    val results: List<Game> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val recent: List<String> = emptyList(),
    val genres: List<Named> = emptyList(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val games: GameRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private val queries = MutableStateFlow("")
    private var job: Job? = null

    init {
        viewModelScope.launch {
            preferences.preferences.map { it.recentSearches }.distinctUntilChanged().collect { recent -> _state.update { it.copy(recent = recent) } }
        }
        viewModelScope.launch { games.genres().onSuccess { list -> _state.update { it.copy(genres = list) } } }
        viewModelScope.launch {
            queries.debounce(DEBOUNCE_MS).distinctUntilChanged().collect { q ->
                if (q.length >= MIN_QUERY) load(reset = true) else if (_state.value.browsing == null) clearResults()
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, browsing = null) }
        queries.value = query.trim()
    }

    /** Explicit submit: search now and remember the query. */
    fun submit() {
        val q = _state.value.query.trim()
        if (q.length < MIN_QUERY) return
        viewModelScope.launch { preferences.addRecentSearch(q) }
        load(reset = true)
    }

    fun useRecent(query: String) {
        _state.update { it.copy(query = query, browsing = null) }
        queries.value = query
        load(reset = true)
    }

    fun clearRecent() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    fun browse(genre: Named) {
        _state.update { it.copy(browsing = genre, query = "") }
        queries.value = ""
        load(reset = true)
    }

    fun clear() {
        _state.update { it.copy(query = "", browsing = null) }
        queries.value = ""
        clearResults()
    }

    fun loadMore() {
        val s = _state.value
        if (s.hasMore && !s.isLoading) load(reset = false)
    }

    private fun clearResults() {
        job?.cancel()
        _state.update { it.copy(results = emptyList(), page = 0, hasMore = false, searched = false, error = null, isLoading = false) }
    }

    private fun load(reset: Boolean) {
        job?.cancel()
        val s = _state.value
        val page = if (reset) 1 else s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        job = viewModelScope.launch {
            val platforms = preferences.preferences.first().platforms
            val query = if (s.browsing != null) {
                GameQuery(genreIds = setOf(s.browsing.id), platforms = platforms, ordering = GameQuery.Ordering.POPULAR, page = page, pageSize = PAGE_SIZE)
            } else {
                // Search isn't narrowed to the user's platforms: looking up a specific game should always find it.
                GameQuery(search = s.query.trim(), page = page, pageSize = PAGE_SIZE)
            }
            games.games(query)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            results = if (reset) result.items else (it.results + result.items).distinctBy { g -> g.id },
                            page = page,
                            hasMore = result.hasMore,
                            isLoading = false,
                            searched = true,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toUserMessage(), searched = true) } }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 350L
        const val MIN_QUERY = 2
        const val PAGE_SIZE = 20
    }
}
