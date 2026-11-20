package com.myth.playpick.ui.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.Checklist
import com.myth.playpick.domain.model.ChecklistItem
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.repository.ChecklistRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.ui.common.toUserMessage
import com.myth.playpick.ui.navigation.ChecklistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChecklistFilter(val label: String) { ALL("All"), LOCKED("To do"), UNLOCKED("Done") }

enum class ChecklistSort(val label: String) { DEFAULT("Game order"), RAREST("Rarest first"), COMMONEST("Easiest first") }

data class ChecklistControls(
    val filter: ChecklistFilter = ChecklistFilter.ALL,
    val sort: ChecklistSort = ChecklistSort.DEFAULT,
    val query: String = "",
)

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val entry: LibraryEntry? = null,
    val controls: ChecklistControls = ChecklistControls(),
    val visible: List<ChecklistItem> = emptyList(),
    val isSyncing: Boolean = false,
    val error: String? = null,
    /** Shown once every achievement is ticked and the game isn't marked 100% yet. */
    val offerMastered: Boolean = false,
)

/**
 * A game's achievements as a checklist. The list is stored on first open, so ticking works offline;
 * refreshing re-fetches it and keeps every tick, matched by achievement id.
 */
@HiltViewModel
class ChecklistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checklists: ChecklistRepository,
    private val library: LibraryRepository,
    private val games: GameRepository,
) : ViewModel() {

    val gameId: Int = checkNotNull(savedStateHandle[ChecklistRoute::gameId.name])
    val name: String = savedStateHandle[ChecklistRoute::name.name] ?: ""

    private val controls = MutableStateFlow(ChecklistControls())
    private val status = MutableStateFlow(SyncStatus())
    private val masteredDismissed = MutableStateFlow(false)

    private data class SyncStatus(val syncing: Boolean = false, val error: String? = null)

    val state: StateFlow<ChecklistUiState> = combine(
        checklists.checklist(gameId),
        library.entry(gameId),
        controls,
        status,
        masteredDismissed,
    ) { checklist, entry, c, s, dismissed ->
        ChecklistUiState(
            checklist = checklist,
            entry = entry,
            controls = c,
            visible = arrange(checklist.items, c),
            isSyncing = s.syncing,
            error = s.error,
            offerMastered = !dismissed && checklist.progress.isComplete && entry?.status != LibraryStatus.MASTERED,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChecklistUiState(isSyncing = true))

    init {
        viewModelScope.launch {
            // Tracking a game puts it in the library, so its progress shows there.
            if (library.entry(gameId).first() == null) {
                games.details(gameId).onSuccess { library.setStatus(it.game, LibraryStatus.PLAYING) }
            }
            if (checklists.checklist(gameId).first().syncedAtMillis == null) sync()
        }
    }

    fun refresh() {
        viewModelScope.launch { sync() }
    }

    private suspend fun sync() {
        status.value = SyncStatus(syncing = true)
        val result = checklists.sync(gameId)
        status.value = SyncStatus(error = result.exceptionOrNull()?.toUserMessage())
    }

    fun toggle(item: ChecklistItem) {
        viewModelScope.launch { checklists.setUnlocked(gameId, item.achievement.id, !item.unlocked) }
    }

    fun setFilter(filter: ChecklistFilter) = controls.update { it.copy(filter = filter) }
    fun setSort(sort: ChecklistSort) = controls.update { it.copy(sort = sort) }
    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun markMastered() {
        val game = state.value.entry?.game ?: return
        viewModelScope.launch { library.setStatus(game, LibraryStatus.MASTERED) }
    }

    fun dismissMastered() {
        masteredDismissed.value = true
    }

    companion object {
        internal fun arrange(items: List<ChecklistItem>, controls: ChecklistControls): List<ChecklistItem> {
            val q = controls.query.trim()
            val filtered = items.filter { item ->
                when (controls.filter) {
                    ChecklistFilter.ALL -> true
                    ChecklistFilter.LOCKED -> !item.unlocked
                    ChecklistFilter.UNLOCKED -> item.unlocked
                } && (q.isEmpty() || item.achievement.name.contains(q, ignoreCase = true) || item.achievement.description.contains(q, ignoreCase = true))
            }
            // Unknown rarity sorts last either way.
            return when (controls.sort) {
                ChecklistSort.DEFAULT -> filtered
                ChecklistSort.RAREST -> filtered.sortedBy { it.achievement.percent ?: Double.MAX_VALUE }
                ChecklistSort.COMMONEST -> filtered.sortedByDescending { it.achievement.percent ?: -1.0 }
            }
        }
    }
}
