package com.myth.playpick.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameDetails
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.recommend.GameTasteProfile
import com.myth.playpick.domain.recommend.GetTasteProfileUseCase
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import com.myth.playpick.ui.common.toUserMessage
import com.myth.playpick.ui.navigation.GameRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the previous screen already knew, so the header draws before details load. */
data class GamePreview(val name: String, val coverUrl: String?)

/** Why this game fits, from the user's own profile: a match and the genres/tags behind it. */
data class TasteFit(val match: Int, val reasons: List<String>)

sealed interface DetailsContent {
    data object Loading : DetailsContent
    data class Error(val message: String) : DetailsContent
    data class Loaded(
        val details: GameDetails,
        val fit: TasteFit?,
        /** The rarest few, to show what 100% asks of you. */
        val rarest: List<Achievement> = emptyList(),
        val series: List<Game> = emptyList(),
        val additions: List<Game> = emptyList(),
        val similar: List<Pair<Game, Int?>> = emptyList(),
    ) : DetailsContent
}

@HiltViewModel
class GameDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val games: GameRepository,
    private val feedback: FeedbackRepository,
    private val library: LibraryRepository,
    private val preferences: UserPreferencesRepository,
    private val tasteProfile: GetTasteProfileUseCase,
) : ViewModel() {

    // Type-safe routes keep each argument under its property name, so they're read directly:
    // unlike toRoute(), this needs no Android Bundle, so the view model is JVM-testable.
    val gameId: Int = checkNotNull(savedStateHandle[GameRoute::gameId.name])
    val sharedKey: String? = savedStateHandle[GameRoute::sharedKey.name]
    val preview = GamePreview(savedStateHandle[GameRoute::name.name] ?: "", savedStateHandle[GameRoute::coverUrl.name])

    private val _content = MutableStateFlow<DetailsContent>(DetailsContent.Loading)
    val content: StateFlow<DetailsContent> = _content.asStateFlow()

    val rating: StateFlow<FeedbackType?> = feedback.feedbackFor(gameId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val libraryEntry: StateFlow<LibraryEntry?> = library.entry(gameId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        load()
    }

    fun load() {
        _content.value = DetailsContent.Loading
        viewModelScope.launch {
            games.details(gameId)
                .onSuccess { details ->
                    // A game's own rating mustn't inflate its own match.
                    val profile = tasteProfile(excluding = gameId)
                    _content.value = DetailsContent.Loaded(details, fit(details.game, profile))
                    loadExtras(details, profile)
                }
                .onFailure { _content.value = DetailsContent.Error(it.toUserMessage()) }
        }
    }

    /** Series, DLC, similar games and achievements are extras: each fills in when it arrives. */
    private suspend fun loadExtras(details: GameDetails, profile: GameTasteProfile) = coroutineScope {
        val platforms = preferences.preferences.first().platforms
        val series = async { if (details.seriesCount > 0) games.series(gameId).getOrDefault(emptyList()) else emptyList() }
        val additions = async { if (details.additionsCount > 0) games.additions(gameId).getOrDefault(emptyList()) else emptyList() }
        val achievements = async { if (details.achievementCount > 0) games.achievements(gameId).getOrDefault(emptyList()) else emptyList() }
        val similar = async {
            val tags = details.game.tags.take(2).map { it.id }.toSet()
            if (tags.isEmpty()) emptyList()
            else games.games(GameQuery(tagIds = tags, platforms = platforms, ordering = GameQuery.Ordering.POPULAR, pageSize = 20))
                .getOrDefault(null)?.items.orEmpty()
                .filter { it.id != gameId }
                .map { it to profile.matchPercent(it.features) }
                .sortedByDescending { it.second ?: 0 }
        }
        val rarest = achievements.await().filter { it.percent != null }.sortedBy { it.percent }.take(RAREST)
        _content.update { current ->
            (current as? DetailsContent.Loaded)?.copy(
                rarest = rarest,
                series = series.await().filter { it.id != gameId },
                additions = additions.await(),
                similar = similar.await(),
            ) ?: current
        }
    }

    fun rate(type: FeedbackType) {
        val game = (content.value as? DetailsContent.Loaded)?.details?.game ?: return
        viewModelScope.launch { feedback.setFeedback(game, if (rating.value == type) null else type) }
    }

    fun setStatus(status: LibraryStatus?) {
        val game = (content.value as? DetailsContent.Loaded)?.details?.game ?: return
        viewModelScope.launch { if (status == null) library.remove(gameId) else library.setStatus(game, status) }
    }

    companion object {
        const val RAREST = 3

        internal fun fit(game: Game, profile: GameTasteProfile): TasteFit? {
            val match = profile.matchPercent(game.features) ?: return null
            val names = (game.genres.map { GameFeature.genre(it.id) to it.name } + game.tags.map { GameFeature.tag(it.id) to it.name })
                .filter { profile.weightOf(it.first) > 0.0 }
                .sortedByDescending { profile.weightOf(it.first) }
                .map { it.second }
                .take(3)
            return TasteFit(match, names)
        }
    }
}
