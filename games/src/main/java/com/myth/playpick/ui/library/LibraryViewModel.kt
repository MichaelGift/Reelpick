package com.myth.playpick.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibrarySection(val status: LibraryStatus, val entries: List<LibraryEntry>)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val sections: List<LibrarySection> = emptyList(),
    /** Null shows every status. */
    val filter: LibraryStatus? = null,
    val counts: Map<LibraryStatus, Int> = emptyMap(),
    /** Achievements unlocked across the whole library. */
    val unlockedTotal: Int = 0,
) {
    val isEmpty: Boolean get() = !isLoading && counts.values.sum() == 0
}

@HiltViewModel
class LibraryViewModel @Inject constructor(private val library: LibraryRepository) : ViewModel() {

    private val filter = MutableStateFlow<LibraryStatus?>(null)

    val state: StateFlow<LibraryUiState> = combine(library.entries, filter) { entries, f ->
        LibraryUiState(
            isLoading = false,
            sections = sections(entries, f),
            filter = f,
            counts = entries.groupingBy { it.status }.eachCount(),
            unlockedTotal = entries.sumOf { it.progress.unlocked },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    fun setFilter(status: LibraryStatus?) {
        filter.value = if (filter.value == status) null else status
    }

    fun setStatus(entry: LibraryEntry, status: LibraryStatus) {
        viewModelScope.launch { library.setStatus(entry.game, status) }
    }

    fun remove(entry: LibraryEntry) {
        viewModelScope.launch { library.remove(entry.game.id) }
    }

    companion object {
        /** What you're playing comes first, then what's next, then what's done. */
        val ORDER = listOf(LibraryStatus.PLAYING, LibraryStatus.BACKLOG, LibraryStatus.COMPLETED, LibraryStatus.MASTERED, LibraryStatus.DROPPED)

        internal fun sections(entries: List<LibraryEntry>, filter: LibraryStatus?): List<LibrarySection> =
            ORDER.filter { filter == null || it == filter }
                .map { status -> LibrarySection(status, entries.filter { it.status == status }) }
                .filter { it.entries.isNotEmpty() }
    }
}
