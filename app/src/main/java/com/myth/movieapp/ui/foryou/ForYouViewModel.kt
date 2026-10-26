package com.myth.movieapp.ui.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import com.myth.movieapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForYouUiState(
    val feed: ForYouFeed? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Shown full-screen when there is no feed yet, otherwise as a snackbar over the cached feed. */
    val error: String? = null,
    val watchlistIds: Set<Int> = emptySet(),
    /** One-off confirmation, e.g. after saving from the feed (saved movies leave the feed). */
    val notice: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val getFeed: GetForYouFeedUseCase,
    private val feedback: FeedbackRepository,
    private val watchlist: WatchlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForYouUiState())
    val state: StateFlow<ForYouUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        // Show the cached feed immediately (offline-first), then recompute.
        getFeed.cachedFeed.filterNotNull()
            .onEach { cached -> _state.update { it.copy(feed = cached, isLoading = false) } }
            .launchIn(viewModelScope)

        watchlist.watchlist
            .onEach { list -> _state.update { it.copy(watchlistIds = list.mapTo(mutableSetOf()) { m -> m.id }) } }
            .launchIn(viewModelScope)

        // Any rating or watchlist change re-ranks; the use case decides whether that needs the network.
        combine(feedback.feedback, watchlist.watchlist) { _, _ -> }
            .drop(1)
            .debounce(400)
            .onEach { load(forceRefresh = false, userInitiated = false) }
            .launchIn(viewModelScope)

        load(forceRefresh = false, userInitiated = false)
    }

    fun refresh() = load(forceRefresh = true, userInitiated = true)

    fun retry() = load(forceRefresh = true, userInitiated = false)

    fun onFeedback(movie: Movie, type: FeedbackType) {
        viewModelScope.launch { feedback.setFeedback(movie, type) }
    }

    fun addToWatchlist(movie: Movie) {
        viewModelScope.launch { watchlist.add(movie) }
        _state.update { it.copy(notice = "Saved “${movie.title}” to your watchlist") }
    }

    fun toggleWatchlist(movie: Movie) {
        if (movie.id in _state.value.watchlistIds) {
            viewModelScope.launch { watchlist.remove(movie.id) }
        } else {
            addToWatchlist(movie)
        }
    }

    fun messageShown() = _state.update { it.copy(error = null, notice = null) }

    private fun load(forceRefresh: Boolean, userInitiated: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(isRefreshing = userInitiated, isLoading = it.feed == null, error = null) }
        loadJob = viewModelScope.launch {
            getFeed(forceRefresh)
                .onSuccess { feed -> _state.update { it.copy(feed = feed, isLoading = false, isRefreshing = false) } }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.toUserMessage()) }
                }
        }
    }
}
