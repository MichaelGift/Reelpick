package com.myth.playpick.domain.recommend

import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.testing.entry
import com.myth.playpick.testing.game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTasteTest {

    private val rpg = 5
    private val metroidvania = 259
    private val sports = 15

    @Test
    fun `finishing outweighs liking and 100 percent outweighs both`() {
        val profile = GameTaste.build(
            genreIds = emptySet(),
            tagIds = emptySet(),
            feedback = listOf(Feedback(game(1, genres = listOf(sports)), FeedbackType.LIKE, 1)),
            library = listOf(
                entry(game(2, tags = listOf(metroidvania)), LibraryStatus.MASTERED),
                entry(game(3, genres = listOf(rpg)), LibraryStatus.COMPLETED),
            ),
        )

        assertEquals(1.0, profile.weightOf(GameFeature.tag(metroidvania)), 1e-9)
        assertTrue(profile.weightOf(GameFeature.genre(rpg)) > 0.0)
        assertEquals(profile.weightOf(GameFeature.genre(sports)), profile.weightOf(GameFeature.genre(rpg)), 1e-9)
    }

    @Test
    fun `dropping a game counts against its genres`() {
        val profile = GameTaste.build(
            genreIds = setOf(rpg),
            tagIds = emptySet(),
            feedback = emptyList(),
            library = listOf(entry(game(1, genres = listOf(sports)), LibraryStatus.DROPPED)),
        )

        assertTrue(profile.weightOf(GameFeature.genre(sports)) < 0.0)
        assertEquals(listOf(GameFeature.genre(rpg)), profile.top(5))
    }

    @Test
    fun `genre and tag ids never collide`() {
        val profile = GameTaste.build(genreIds = setOf(4), tagIds = emptySet(), feedback = emptyList(), library = emptyList())

        assertTrue(profile.weightOf(GameFeature.genre(4)) > 0.0)
        assertEquals(0.0, profile.weightOf(GameFeature.tag(4)), 1e-9)
    }

    @Test
    fun `seeds are likes and played games, newest first, and everything judged is excluded`() {
        val snapshot = TasteSnapshot(
            profile = GameTaste.build(emptySet(), emptySet(), emptyList(), emptyList()),
            feedback = listOf(
                Feedback(game(1), FeedbackType.LIKE, createdAtMillis = 10),
                Feedback(game(2), FeedbackType.DISLIKE, createdAtMillis = 50),
            ),
            library = listOf(
                entry(game(3), LibraryStatus.MASTERED, updatedAt = 40),
                entry(game(4), LibraryStatus.BACKLOG, updatedAt = 60),
                entry(game(5), LibraryStatus.PLAYING, updatedAt = 20),
            ),
        )

        assertEquals(listOf(3, 5, 1), snapshot.seeds.map { it.id })
        assertEquals(setOf(1, 2, 3, 4, 5), snapshot.excludedIds)
    }

    @Test
    fun `ratings out of five become the engine's ten-point scale`() {
        assertEquals(8.8, GameTraits.rating(game(1, rating = 4.4)), 1e-9)
    }
}
