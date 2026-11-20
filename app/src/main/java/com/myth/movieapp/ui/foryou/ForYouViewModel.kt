package com.myth.movieapp.ui.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.FeedScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import com.myth.movieapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForYouUiState(
    val scope: FeedScope = FeedScope.ALL,
    val feed: ForYouFeed? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Shown full-screen when there is no feed yet, otherwise as a snackbar over the cached feed. */
    val error: String? = null,
    val watchlistIds: Set<TitleKey> = emptySet(),
    /** One-off confirmation, e.g. after saving from the feed (saved titles leave the feed). */
    val notice: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val getFeed: GetForYouFeedUseCase,
    private val feedback: FeedbackRepository,
    private val watchlist: WatchlistRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForYouUiState())
    val state: StateFlow<ForYouUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        // Show the selected scope's cached feed immediately (offline-first), then recompute.
        _state.map { it.scope }.distinctUntilChanged()
            .flatMapLatest { scope -> getFeed.cachedFeed(scope).filterNotNull().map { scope to it } }
            .onEach { (scope, cached) ->
                _state.update { if (it.scope == scope) it.copy(feed = cached, isLoading = false) else it }
            }
            .launchIn(viewModelScope)

        watchlist.watchlist
            .onEach { list -> _state.update { it.copy(watchlistIds = list.mapTo(mutableSetOf()) { m -> m.key }) } }
            .launchIn(viewModelScope)

        // Any rating or watchlist change re-ranks; the use case decides whether that needs the network.
        combine(feedback.feedback, watchlist.watchlist) { _, _ -> }
            .drop(1)
            .debounce(400)
            .onEach { load(forceRefresh = false, userInitiated = false) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _state.update { it.copy(scope = preferences.preferences.first().feedScope) }
            load(forceRefresh = false, userInitiated = false)
        }
    }

    /** Switches between All, Movies and Series. Instant: every scope ranks the same candidate pool. */
    fun setScope(scope: FeedScope) {
        if (scope == _state.value.scope) return
        _state.update { it.copy(scope = scope, feed = null, isLoading = true, error = null) }
        viewModelScope.launch { preferences.setFeedScope(scope) }
        load(forceRefresh = false, userInitiated = false)
    }

    fun refresh() = load(forceRefresh = true, userInitiated = true)

    fun retry() = load(forceRefresh = true, userInitiated = false)

    fun onFeedback(title: Title, type: FeedbackType) {
        viewModelScope.launch { feedback.setFeedback(title, type) }
    }

    fun addToWatchlist(title: Title) {
        viewModelScope.launch { watchlist.add(title) }
        _state.update { it.copy(notice = "Saved “${title.title}” to your watchlist") }
    }

    fun toggleWatchlist(title: Title) {
        if (title.key in _state.value.watchlistIds) {
            viewModelScope.launch { watchlist.remove(title.key) }
        } else {
            addToWatchlist(title)
        }
    }

    fun messageShown() = _state.update { it.copy(error = null, notice = null) }

    private fun load(forceRefresh: Boolean, userInitiated: Boolean) {
        loadJob?.cancel()
        val scope = _state.value.scope
        _state.update { it.copy(isRefreshing = userInitiated, isLoading = it.feed == null, error = null) }
        loadJob = viewModelScope.launch {
            getFeed(scope, forceRefresh)
                .onSuccess { feed ->
                    _state.update { if (it.scope == scope) it.copy(feed = feed, isLoading = false, isRefreshing = false) else it }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.toUserMessage()) }
                }
        }
    }
}
