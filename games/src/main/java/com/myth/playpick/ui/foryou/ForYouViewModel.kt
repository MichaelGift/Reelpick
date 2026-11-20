package com.myth.playpick.ui.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.recommend.GameFeed
import com.myth.playpick.domain.recommend.GetForYouFeedUseCase
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import com.myth.playpick.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForYouUiState(
    val feed: GameFeed? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Null means all of the user's platforms. */
    val scope: Platform? = null,
    val platforms: List<Platform> = emptyList(),
)

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val getFeed: GetForYouFeedUseCase,
    private val preferences: UserPreferencesRepository,
    private val feedback: FeedbackRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForYouUiState())
    val state: StateFlow<ForYouUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferences.preferences.first()
            _state.update { it.copy(scope = prefs.feedPlatform, platforms = prefs.platforms.sortedBy { p -> p.ordinal }) }
            // The cached feed shows instantly; a fresh one replaces it when ready.
            getFeed.cachedFeed(prefs.feedPlatform)?.let { cached -> _state.update { it.copy(feed = cached, isLoading = false) } }
            load(forceRefresh = false)
        }
        viewModelScope.launch {
            preferences.preferences.map { it.platforms }.distinctUntilChanged().collect { platforms ->
                _state.update { it.copy(platforms = platforms.sortedBy { p -> p.ordinal }) }
            }
        }
    }

    fun setScope(scope: Platform?) {
        if (scope == _state.value.scope) return
        _state.update { it.copy(scope = scope) }
        viewModelScope.launch {
            preferences.setFeedPlatform(scope)
            getFeed.cachedFeed(scope)?.let { cached -> _state.update { it.copy(feed = cached) } }
            load(forceRefresh = false)
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch { load(forceRefresh = true) }
    }

    fun rate(game: Game, type: FeedbackType) {
        viewModelScope.launch {
            feedback.setFeedback(game, type)
            load(forceRefresh = false)
        }
    }

    fun addToLibrary(game: Game, status: LibraryStatus) {
        viewModelScope.launch {
            library.setStatus(game, status)
            load(forceRefresh = false)
        }
    }

    private suspend fun load(forceRefresh: Boolean) {
        getFeed(_state.value.scope, forceRefresh)
            .onSuccess { feed -> _state.update { it.copy(feed = feed, isLoading = false, isRefreshing = false, error = null) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.toUserMessage()) } }
    }
}
