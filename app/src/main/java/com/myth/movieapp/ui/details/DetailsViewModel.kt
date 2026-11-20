package com.myth.movieapp.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.recommend.GetTasteProfileUseCase
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.TitleRepository
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

/** What the list screen already knew about the movie, so the header can render instantly. */
data class DetailsPreview(val title: String, val posterPath: String?, val backdropPath: String?)

/** How well the movie fits the user's taste profile, with the genres that drive it. */
data class TasteFit(val match: Int, val sharedGenres: List<String>)

sealed interface DetailsContent {
    data object Loading : DetailsContent
    data class Error(val message: String) : DetailsContent
    data class Loaded(
        val details: TitleDetails,
        val moreLikeThis: List<Title>,
        val region: String,
        val fit: TasteFit?,
    ) : DetailsContent
}

data class DetailsUiState(
    val content: DetailsContent = DetailsContent.Loading,
    val feedback: FeedbackType? = null,
    val inWatchlist: Boolean = false,
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movies: TitleRepository,
    private val feedback: FeedbackRepository,
    private val watchlist: WatchlistRepository,
    private val preferences: UserPreferencesRepository,
    private val tasteProfile: GetTasteProfileUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DetailsRoute>()
    // Movies only until the route carries a media type.
    private val key = TitleKey(MediaType.MOVIE, route.movieId)
    /** Key of the poster that was tapped, so the details poster can morph out of it. */
    val sharedKey: String? = route.sharedKey
    val preview = DetailsPreview(route.title, route.posterPath, route.backdropPath)

    private val content = MutableStateFlow<DetailsContent>(DetailsContent.Loading)

    val state: StateFlow<DetailsUiState> = combine(
        content,
        feedback.feedbackFor(key),
        watchlist.isInWatchlist(key),
    ) { content, feedbackType, inWatchlist -> DetailsUiState(content, feedbackType, inWatchlist) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    init {
        load()
    }

    fun load() {
        content.value = DetailsContent.Loading
        viewModelScope.launch {
            val prefs = preferences.preferences.first()
            val region = prefs.region
                ?: Locale.getDefault().country.takeIf { it.length == 2 }
                ?: "US"
            movies.details(key, region)
                .onSuccess { details ->
                    val fit = tasteFit(details)
                    content.value = DetailsContent.Loaded(details, emptyList(), region, fit)
                    // "More like this" is a nice-to-have; failures here don't block the screen.
                    movies.recommendationsFor(key).onSuccess { similar ->
                        content.value = DetailsContent.Loaded(details, similar.filter { it.posterPath != null }, region, fit)
                    }
                }
                .onFailure { content.value = DetailsContent.Error(it.toUserMessage()) }
        }
    }

    private suspend fun tasteFit(details: TitleDetails): TasteFit? {
        // Leave this title's own rating out so liking it doesn't inflate its own match.
        val profile = tasteProfile(excluding = key)
        val match = profile.matchPercent(details.title.genreIds) ?: return null
        // Name genres from the shared space (a series' "Sci-Fi & Fantasy" is Science Fiction + Fantasy).
        val names = movies.genres().getOrDefault(emptyList()).associate { it.id to it.name }
        val shared = details.title.genreIds
            .filter { profile.weightOf(it) > 0.0 }
            .sortedByDescending { profile.weightOf(it) }
            .mapNotNull { names[it] }
            .take(3)
        return TasteFit(match, shared)
    }

    /** Tapping the active rating again clears it. */
    fun toggleFeedback(type: FeedbackType) {
        val movie = loadedTitle() ?: return
        viewModelScope.launch {
            if (state.value.feedback == type) feedback.clearFeedback(movie.key) else feedback.setFeedback(movie, type)
        }
    }

    fun toggleWatchlist() {
        val movie = loadedTitle() ?: return
        viewModelScope.launch {
            if (state.value.inWatchlist) watchlist.remove(movie.key) else watchlist.add(movie)
        }
    }

    private fun loadedTitle(): Title? = (content.value as? DetailsContent.Loaded)?.details?.title
}
