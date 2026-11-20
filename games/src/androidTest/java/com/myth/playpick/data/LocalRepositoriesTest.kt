package com.myth.playpick.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.myth.playpick.data.local.AppDatabase
import com.myth.playpick.data.repository.ChecklistRepositoryImpl
import com.myth.playpick.data.repository.FeedbackRepositoryImpl
import com.myth.playpick.data.repository.LibraryRepositoryImpl
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameDetails
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Paged
import com.myth.playpick.domain.repository.GameRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class LocalRepositoriesTest {

    /** Only achievements matter here; RAWG's list can change between syncs. */
    private class FakeGames(var list: List<Achievement>) : GameRepository {
        override suspend fun achievements(id: Int) = Result.success(list)
        override suspend fun games(query: GameQuery): Result<Paged<Game>> = error("unused")
        override suspend fun details(id: Int): Result<GameDetails> = error("unused")
        override suspend fun series(id: Int): Result<List<Game>> = error("unused")
        override suspend fun additions(id: Int): Result<List<Game>> = error("unused")
        override suspend fun genres(): Result<List<Named>> = error("unused")
        override suspend fun tags(): Result<List<Named>> = error("unused")
    }

    private lateinit var db: AppDatabase
    private val json = Json { ignoreUnknownKeys = true }
    private var now = 1_000L
    private val clock get() = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)

    private val game = Game(9767, "hollow-knight", "Hollow Knight", genres = listOf(Named(4, "Action")))
    private fun achievement(id: Long) = Achievement(id, "A$id", "Do thing $id", percent = id.toDouble())

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun resyncKeepsTicksMatchedById() = runTest {
        val rawg = FakeGames(listOf(achievement(1), achievement(2), achievement(3)))
        val checklists = ChecklistRepositoryImpl(db.checklistDao(), rawg, clock)

        assertNull(checklists.checklist(game.id).first().syncedAtMillis)
        checklists.sync(game.id).getOrThrow()
        checklists.setUnlocked(game.id, 1, true)
        checklists.setUnlocked(game.id, 3, true)

        // RAWG reorders the list, drops achievement 3 and adds 4.
        rawg.list = listOf(achievement(4), achievement(2), achievement(1))
        checklists.sync(game.id).getOrThrow()
        val list = checklists.checklist(game.id).first()

        assertEquals(listOf(4L, 2L, 1L), list.items.map { it.achievement.id })
        assertEquals(listOf(false, false, true), list.items.map { it.unlocked })
        assertEquals(1, list.progress.unlocked)
        assertEquals(3, list.progress.total)
        assertNotNull(list.syncedAtMillis)
    }

    @Test
    fun untickingRemovesTheTick() = runTest {
        val checklists = ChecklistRepositoryImpl(db.checklistDao(), FakeGames(listOf(achievement(1))), clock)
        checklists.sync(game.id)

        checklists.setUnlocked(game.id, 1, true)
        checklists.setUnlocked(game.id, 1, true) // a double tap keeps the first time
        assertTrue(checklists.checklist(game.id).first().items.single().unlocked)

        checklists.setUnlocked(game.id, 1, false)
        assertFalse(checklists.checklist(game.id).first().items.single().unlocked)
    }

    @Test
    fun libraryRowsCarryChecklistProgressAndKeepTheirAddedTime() = runTest {
        val library = LibraryRepositoryImpl(db.libraryDao(), json, clock)
        val checklists = ChecklistRepositoryImpl(db.checklistDao(), FakeGames(listOf(achievement(1), achievement(2))), clock)

        library.setStatus(game, LibraryStatus.PLAYING)
        checklists.sync(game.id)
        checklists.setUnlocked(game.id, 2, true)
        now = 5_000L
        val later = LibraryRepositoryImpl(db.libraryDao(), json, clock)
        later.setStatus(game, LibraryStatus.COMPLETED)

        val entry = later.entries.first().single()
        assertEquals(LibraryStatus.COMPLETED, entry.status)
        assertEquals(1_000L, entry.addedAtMillis)
        assertEquals(5_000L, entry.updatedAtMillis)
        assertEquals(1, entry.progress.unlocked)
        assertEquals(2, entry.progress.total)
        assertEquals("Hollow Knight", entry.game.name)

        later.remove(game.id)
        assertNull(later.entry(game.id).first())
    }

    @Test
    fun feedbackRoundTripsAndClears() = runTest {
        val feedback = FeedbackRepositoryImpl(db.feedbackDao(), json, clock)

        feedback.setFeedback(game, FeedbackType.LIKE)
        assertEquals(FeedbackType.LIKE, feedback.feedbackFor(game.id).first())
        assertEquals(listOf("Hollow Knight"), feedback.feedback.first().map { it.game.name })

        feedback.setFeedback(game, null)
        assertNull(feedback.feedbackFor(game.id).first())
    }
}
