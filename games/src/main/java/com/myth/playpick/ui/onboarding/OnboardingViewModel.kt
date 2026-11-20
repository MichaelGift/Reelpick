package com.myth.playpick.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import com.myth.playpick.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: OnboardingViewModel.Step = OnboardingViewModel.Step.Welcome,
    val mosaic: List<Game> = emptyList(),
    val platforms: Set<Platform> = emptySet(),
    val genres: List<Named> = emptyList(),
    val tags: List<Named> = emptyList(),
    val selectedGenres: Set<Int> = emptySet(),
    val selectedTags: Set<Int> = emptySet(),
    val games: List<Game> = emptyList(),
    val selectedGames: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFinishing: Boolean = false,
) {
    val tasteCount: Int get() = selectedGenres.size + selectedTags.size
}

/** Platforms → genres & tags → five games you loved. Those games become the first "seeds". */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val games: GameRepository,
    private val feedback: FeedbackRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    enum class Step { Welcome, Platforms, Taste, Games }

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            games.games(GameQuery(ordering = GameQuery.Ordering.POPULAR, pageSize = 24))
                .onSuccess { page -> _state.update { it.copy(mosaic = page.items.filter { g -> g.coverUrl != null }) } }
        }
    }

    fun start() = go(Step.Platforms)

    fun back() {
        val previous = Step.entries.getOrNull(_state.value.step.ordinal - 1) ?: return
        go(previous)
    }

    fun togglePlatform(platform: Platform) = _state.update { it.copy(platforms = it.platforms.toggle(platform)) }

    fun toPlatformsDone() {
        if (_state.value.platforms.isEmpty()) return
        go(Step.Taste)
        if (_state.value.genres.isEmpty()) loadTaste()
    }

    fun toggleGenre(id: Int) = _state.update { it.copy(selectedGenres = it.selectedGenres.toggle(id)) }

    fun toggleTag(id: Int) = _state.update { it.copy(selectedTags = it.selectedTags.toggle(id)) }

    fun loadTaste() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val genres = async { games.genres() }
            val tags = async { games.tags() }
            val g = genres.await()
            val t = tags.await()
            _state.update {
                it.copy(
                    genres = g.getOrDefault(emptyList()),
                    tags = t.getOrDefault(emptyList()).take(MAX_TAGS),
                    isLoading = false,
                    error = g.exceptionOrNull()?.toUserMessage(),
                )
            }
        }
    }

    fun toTasteDone() {
        if (_state.value.tasteCount < MIN_TASTE) return
        go(Step.Games)
        loadGames()
    }

    /** Popular games on the chosen platforms in the chosen genres and tags; RAWG treats each list as "any of". */
    fun loadGames() {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val base = GameQuery(platforms = s.platforms, ordering = GameQuery.Ordering.POPULAR, pageSize = 24)
            val byGenre = async { if (s.selectedGenres.isEmpty()) Result.success(emptyList()) else games.games(base.copy(genreIds = s.selectedGenres)).map { it.items } }
            val byTag = async { if (s.selectedTags.isEmpty()) Result.success(emptyList()) else games.games(base.copy(tagIds = s.selectedTags)).map { it.items } }
            val results = listOf(byGenre.await(), byTag.await())
            val merged = interleave(results.map { it.getOrDefault(emptyList()) }).distinctBy { it.id }
            _state.update {
                it.copy(
                    games = merged,
                    isLoading = false,
                    error = if (merged.isEmpty()) results.firstNotNullOfOrNull { r -> r.exceptionOrNull() }?.toUserMessage() else null,
                )
            }
        }
    }

    fun toggleGame(id: Int) = _state.update { it.copy(selectedGames = it.selectedGames.toggle(id)) }

    fun finish() {
        val s = _state.value
        if (s.selectedGames.size < MIN_GAMES || s.isFinishing) return
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            // Liked in reverse so the first-picked game is the newest seed.
            s.games.filter { it.id in s.selectedGames }.reversed().forEach { feedback.setFeedback(it, FeedbackType.LIKE) }
            preferences.completeOnboarding(s.platforms, s.selectedGenres, s.selectedTags)
        }
    }

    private fun go(step: Step) = _state.update { it.copy(step = step, error = null) }

    private fun <T> Set<T>.toggle(item: T) = if (item in this) this - item else this + item

    private fun <T> interleave(lists: List<List<T>>): List<T> =
        (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }

    companion object {
        const val MIN_TASTE = 3
        const val MIN_GAMES = 5
        const val MAX_TAGS = 24
    }
}
