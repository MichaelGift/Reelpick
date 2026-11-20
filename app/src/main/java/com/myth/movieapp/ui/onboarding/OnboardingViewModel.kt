package com.myth.movieapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.TitleRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: Step = Step.Welcome,
    /** Posters for the welcome screen's backdrop; decorative, so failures just leave it empty. */
    val mosaic: List<Title> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val movies: List<Title> = emptyList(),
    val likedIds: Set<TitleKey> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    enum class Step { Welcome, Genres, Movies }

    val canContinue: Boolean get() = selectedGenreIds.size >= MIN_GENRES
    val canFinish: Boolean get() = likedIds.size >= MIN_LIKES && !isSaving

    companion object {
        const val MIN_GENRES = 3
        const val MIN_LIKES = 5
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val movies: TitleRepository,
    private val feedback: FeedbackRepository,
    private val preferences: UserPreferencesRepository,
    private val forYouFeed: GetForYouFeedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        loadGenres()
        viewModelScope.launch {
            movies.popular().onSuccess { page ->
                _state.update { it.copy(mosaic = page.titles.filter { m -> m.posterPath != null }) }
            }
        }
    }

    fun loadGenres() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            movies.genres()
                .onSuccess { genres -> _state.update { it.copy(genres = genres, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun start() = _state.update { it.copy(step = OnboardingUiState.Step.Genres) }

    fun toggleGenre(id: Int) = _state.update {
        val selected = if (id in it.selectedGenreIds) it.selectedGenreIds - id else it.selectedGenreIds + id
        it.copy(selectedGenreIds = selected)
    }

    fun continueToMovies() {
        if (!_state.value.canContinue) return
        _state.update { it.copy(step = OnboardingUiState.Step.Movies, isLoading = true, error = null) }
        viewModelScope.launch {
            val genreIds = _state.value.selectedGenreIds.toList()
            // Popular films and series per chosen genre, interleaved so every genre and both types
            // show up near the top.
            val perGenre = genreIds.flatMap { id ->
                MediaType.entries.map { type -> async { movies.discoverByGenre(type, id) } }
            }.awaitAll()
            val popular = MediaType.entries.map { type -> async { movies.popular(type).map { it.titles } } }.awaitAll()
            val results = perGenre + popular
            val lists = results.mapNotNull { it.getOrNull() }.filter { it.isNotEmpty() }
            if (lists.isEmpty()) {
                val error = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                _state.update { it.copy(isLoading = false, error = error?.toUserMessage()) }
                return@launch
            }
            val interleaved = (0 until lists.maxOf { it.size })
                .flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }
                .filter { it.posterPath != null }
                .distinctBy { it.key }
                .take(MAX_MOVIES)
            _state.update { it.copy(movies = interleaved, isLoading = false) }
        }
    }

    fun back() = _state.update {
        when (it.step) {
            OnboardingUiState.Step.Movies -> it.copy(step = OnboardingUiState.Step.Genres, error = null)
            else -> it.copy(step = OnboardingUiState.Step.Welcome, error = null)
        }
    }

    fun toggleLike(key: TitleKey) = _state.update {
        it.copy(likedIds = if (key in it.likedIds) it.likedIds - key else it.likedIds + key)
    }

    fun finish() {
        val current = _state.value
        if (!current.canFinish) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            current.movies.filter { it.key in current.likedIds }.forEach {
                feedback.setFeedback(it, FeedbackType.LIKE)
            }
            forYouFeed.invalidate()
            // Flipping this flag swaps the app over to the main screens.
            preferences.completeOnboarding(current.selectedGenreIds)
        }
    }

    private companion object {
        const val MAX_MOVIES = 36
    }
}
