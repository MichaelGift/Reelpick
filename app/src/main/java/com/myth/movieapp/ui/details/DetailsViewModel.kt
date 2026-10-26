package com.myth.movieapp.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.MovieRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import com.myth.movieapp.ui.common.toUserMessage
import com.myth.movieapp.ui.navigation.DetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed interface DetailsContent {
    data object Loading : DetailsContent
    data class Error(val message: String) : DetailsContent
    data class Loaded(val details: MovieDetails, val moreLikeThis: List<Movie>, val region: String) : DetailsContent
}

data class DetailsUiState(
    val content: DetailsContent = DetailsContent.Loading,
    val feedback: FeedbackType? = null,
    val inWatchlist: Boolean = false,
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movies: MovieRepository,
    private val feedback: FeedbackRepository,
    private val watchlist: WatchlistRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val movieId = savedStateHandle.toRoute<DetailsRoute>().movieId
    private val content = MutableStateFlow<DetailsContent>(DetailsContent.Loading)

    val state: StateFlow<DetailsUiState> = combine(
        content,
        feedback.feedbackFor(movieId),
        watchlist.isInWatchlist(movieId),
    ) { content, feedbackType, inWatchlist -> DetailsUiState(content, feedbackType, inWatchlist) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    init {
        load()
    }

    fun load() {
        content.value = DetailsContent.Loading
        viewModelScope.launch {
            val region = preferences.preferences.first().region
                ?: Locale.getDefault().country.takeIf { it.length == 2 }
                ?: "US"
            movies.details(movieId, region)
                .onSuccess { details ->
                    content.value = DetailsContent.Loaded(details, emptyList(), region)
                    // "More like this" is a nice-to-have; failures here don't block the screen.
                    movies.recommendationsFor(movieId).onSuccess { similar ->
                        content.value = DetailsContent.Loaded(details, similar.filter { it.posterPath != null }, region)
                    }
                }
                .onFailure { content.value = DetailsContent.Error(it.toUserMessage()) }
        }
    }

    /** Tapping the active rating again clears it. */
    fun toggleFeedback(type: FeedbackType) {
        val movie = loadedMovie() ?: return
        viewModelScope.launch {
            if (state.value.feedback == type) feedback.clearFeedback(movie.id) else feedback.setFeedback(movie, type)
        }
    }

    fun toggleWatchlist() {
        val movie = loadedMovie() ?: return
        viewModelScope.launch {
            if (state.value.inWatchlist) watchlist.remove(movie.id) else watchlist.add(movie)
        }
    }

    private fun loadedMovie(): Movie? = (content.value as? DetailsContent.Loaded)?.details?.movie
}
