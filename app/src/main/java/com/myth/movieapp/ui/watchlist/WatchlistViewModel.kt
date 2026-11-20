package com.myth.movieapp.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlist: WatchlistRepository,
    private val feedback: FeedbackRepository,
) : ViewModel() {

    /** Null while the first Room emission is pending, so the screen doesn't flash "empty". */
    val movies: StateFlow<List<Title>?> = watchlist.watchlist
        .map<List<Title>, List<Title>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(movie: Title) {
        viewModelScope.launch { watchlist.remove(movie.id) }
    }

    fun restore(movie: Title) {
        viewModelScope.launch { watchlist.add(movie) }
    }

    fun markWatched(movie: Title) {
        viewModelScope.launch {
            feedback.setFeedback(movie, FeedbackType.WATCHED)
            watchlist.remove(movie.id)
        }
    }

    fun undoWatched(movie: Title) {
        viewModelScope.launch {
            feedback.clearFeedback(movie.id)
            watchlist.add(movie)
        }
    }
}
