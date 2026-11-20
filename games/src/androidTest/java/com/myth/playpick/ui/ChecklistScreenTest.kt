package com.myth.playpick.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.testing.FakeChecklistRepository
import com.myth.playpick.testing.FakeGameRepository
import com.myth.playpick.testing.FakeLibraryRepository
import com.myth.playpick.ui.checklist.ChecklistScreen
import com.myth.playpick.ui.checklist.ChecklistViewModel
import com.myth.playpick.ui.theme.PlaypickTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChecklistScreenTest {

    @get:Rule val compose = createComposeRule()

    private val rawg = FakeGameRepository().apply {
        achievementsById[9767] = listOf(
            Achievement(1, "Falsehood", "Defeat the False Knight", percent = 72.0),
            Achievement(2, "Heart of Hallownest", "Acquire all other trophies", percent = 1.46),
        )
    }
    private val library = FakeLibraryRepository()

    private fun show() {
        val vm = ChecklistViewModel(SavedStateHandle(mapOf("gameId" to 9767, "name" to "Hollow Knight")), FakeChecklistRepository(rawg), library, rawg)
        compose.setContent { PlaypickTheme { ChecklistScreen(onBack = {}, viewModel = vm) } }
    }

    @Test
    fun tickingAnAchievementUpdatesProgress() {
        show()
        compose.onNodeWithText("0 / 2").assertIsDisplayed()
        compose.onNodeWithText("ULTRA RARE · 1.5%").assertIsDisplayed()

        compose.onNodeWithText("Falsehood").performClick()

        compose.onNodeWithText("1 / 2").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun finishingTheListOffersOneHundredPercent() {
        show()
        compose.onNodeWithText("Falsehood").performClick()
        compose.onNodeWithText("Heart of Hallownest").performClick()

        compose.onNodeWithText("Every achievement unlocked").assertIsDisplayed()
        compose.onNodeWithText("Mark 100%").performClick()

        compose.waitUntil(5_000) { runBlocking { library.entry(9767).first()?.status == LibraryStatus.MASTERED } }
        assertEquals(LibraryStatus.MASTERED, runBlocking { library.entry(9767).first()!!.status })
    }
}
