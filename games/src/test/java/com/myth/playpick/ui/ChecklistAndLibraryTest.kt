package com.myth.playpick.ui

import androidx.lifecycle.SavedStateHandle
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.ChecklistItem
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.testing.FakeChecklistRepository
import com.myth.playpick.testing.FakeGameRepository
import com.myth.playpick.testing.FakeLibraryRepository
import com.myth.playpick.testing.MainDispatcherRule
import com.myth.playpick.testing.entry
import com.myth.playpick.testing.game
import com.myth.playpick.ui.checklist.ChecklistControls
import com.myth.playpick.ui.checklist.ChecklistFilter
import com.myth.playpick.ui.checklist.ChecklistSort
import com.myth.playpick.ui.checklist.ChecklistViewModel
import com.myth.playpick.ui.library.LibraryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistAndLibraryTest {

    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    private fun item(id: Long, percent: Double?, unlocked: Boolean = false, name: String = "A$id") =
        ChecklistItem(Achievement(id, name, "Do $name", percent = percent), if (unlocked) 1L else null)

    private val items = listOf(item(1, 40.0, unlocked = true), item(2, 2.0), item(3, null), item(4, 75.0, name = "Bench"))

    @Test
    fun `filters split to-do from done`() {
        assertEquals(listOf(2L, 3L, 4L), ChecklistViewModel.arrange(items, ChecklistControls(filter = ChecklistFilter.LOCKED)).map { it.achievement.id })
        assertEquals(listOf(1L), ChecklistViewModel.arrange(items, ChecklistControls(filter = ChecklistFilter.UNLOCKED)).map { it.achievement.id })
    }

    @Test
    fun `sorting by rarity puts unknown rarity last both ways`() {
        assertEquals(listOf(2L, 1L, 4L, 3L), ChecklistViewModel.arrange(items, ChecklistControls(sort = ChecklistSort.RAREST)).map { it.achievement.id })
        assertEquals(listOf(4L, 1L, 2L, 3L), ChecklistViewModel.arrange(items, ChecklistControls(sort = ChecklistSort.COMMONEST)).map { it.achievement.id })
    }

    @Test
    fun `search matches names and descriptions`() {
        assertEquals(listOf(4L), ChecklistViewModel.arrange(items, ChecklistControls(query = "bench")).map { it.achievement.id })
    }

    @Test
    fun `opening a checklist syncs it once, adds the game as playing and keeps ticks`() = runTest {
        val rawg = FakeGameRepository().apply { achievementsById[7] = listOf(Achievement(1, "One", ""), Achievement(2, "Two", "")) }
        val checklists = FakeChecklistRepository(rawg)
        val library = FakeLibraryRepository()

        val vm = ChecklistViewModel(SavedStateHandle(mapOf("gameId" to 7, "name" to "Game 7")), checklists, library, rawg)
        advanceUntilIdle()

        assertEquals(1, checklists.syncs)
        assertEquals(LibraryStatus.PLAYING, library.entry(7).first()!!.status)
        assertEquals(2, vm.state.value.checklist!!.items.size)

        vm.toggle(vm.state.value.visible.first()); advanceUntilIdle()
        vm.refresh(); advanceUntilIdle()
        assertEquals(1, vm.state.value.checklist!!.progress.unlocked)
    }

    @Test
    fun `finishing the list offers 100 percent, once`() = runTest {
        val rawg = FakeGameRepository().apply { achievementsById[7] = listOf(Achievement(1, "Only", "")) }
        val library = FakeLibraryRepository()
        val vm = ChecklistViewModel(SavedStateHandle(mapOf("gameId" to 7)), FakeChecklistRepository(rawg), library, rawg)
        advanceUntilIdle()
        assertFalse(vm.state.value.offerMastered)

        vm.toggle(vm.state.value.visible.single()); advanceUntilIdle()
        assertTrue(vm.state.value.offerMastered)

        vm.markMastered(); advanceUntilIdle()
        assertEquals(LibraryStatus.MASTERED, library.entry(7).first()!!.status)
        assertFalse(vm.state.value.offerMastered)
    }

    @Test
    fun `declining 100 percent keeps the prompt away`() = runTest {
        val rawg = FakeGameRepository().apply { achievementsById[7] = listOf(Achievement(1, "Only", "")) }
        val vm = ChecklistViewModel(SavedStateHandle(mapOf("gameId" to 7)), FakeChecklistRepository(rawg), FakeLibraryRepository(), rawg)
        advanceUntilIdle()
        vm.toggle(vm.state.value.visible.single()); advanceUntilIdle()

        vm.dismissMastered(); advanceUntilIdle()

        assertFalse(vm.state.value.offerMastered)
    }

    @Test
    fun `library sections run playing, backlog, then finished, and filter to one`() {
        val entries = listOf(
            entry(game(1), LibraryStatus.COMPLETED),
            entry(game(2), LibraryStatus.PLAYING),
            entry(game(3), LibraryStatus.BACKLOG),
            entry(game(4), LibraryStatus.PLAYING),
        )

        assertEquals(
            listOf(LibraryStatus.PLAYING, LibraryStatus.BACKLOG, LibraryStatus.COMPLETED),
            LibraryViewModel.sections(entries, null).map { it.status },
        )
        assertEquals(listOf(2, 4), LibraryViewModel.sections(entries, LibraryStatus.PLAYING).single().entries.map { it.game.id })
    }
}
