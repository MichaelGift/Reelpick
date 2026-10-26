package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.testing.movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteProfileTest {

    private val action = 28
    private val comedy = 35
    private val horror = 27

    @Test
    fun `empty input gives empty profile with zero affinity`() {
        val profile = TasteProfile.build(emptySet(), emptyList(), emptyList())

        assertTrue(profile.isEmpty)
        assertEquals(0.0, profile.affinity(listOf(action)), 0.0)
        assertEquals(emptyList<Int>(), profile.topGenres(3))
    }

    @Test
    fun `likes outweigh onboarding picks and weights are normalised to one`() {
        val profile = TasteProfile.build(
            selectedGenreIds = setOf(action, comedy),
            feedback = listOf(Feedback(movie(1, listOf(comedy)), FeedbackType.LIKE, 1)),
            watchlist = emptyList(),
        )

        // comedy = 1 + 2 = 3, action = 1  → normalised by 3
        assertEquals(1.0, profile.weightOf(comedy), 1e-9)
        assertEquals(1.0 / 3, profile.weightOf(action), 1e-9)
        assertEquals(listOf(comedy, action), profile.topGenres(3))
    }

    @Test
    fun `dislikes push a genre negative and out of top genres`() {
        val profile = TasteProfile.build(
            selectedGenreIds = setOf(action, horror),
            feedback = listOf(
                Feedback(movie(1, listOf(horror)), FeedbackType.DISLIKE, 1),
                Feedback(movie(2, listOf(horror)), FeedbackType.NOT_INTERESTED, 2),
            ),
            watchlist = emptyList(),
        )

        assertTrue(profile.weightOf(horror) < 0)
        assertEquals(listOf(action), profile.topGenres(3))
        assertTrue(profile.affinity(listOf(horror)) < 0)
    }

    @Test
    fun `watched without a rating carries no signal`() {
        val profile = TasteProfile.build(
            selectedGenreIds = emptySet(),
            feedback = listOf(Feedback(movie(1, listOf(horror)), FeedbackType.WATCHED, 1)),
            watchlist = emptyList(),
        )

        assertTrue(profile.isEmpty)
    }

    @Test
    fun `watchlist counts as a mild positive signal`() {
        val profile = TasteProfile.build(emptySet(), emptyList(), listOf(movie(1, listOf(comedy))))

        assertEquals(listOf(comedy), profile.topGenres(1))
    }

    @Test
    fun `affinity is cosine similarity and favours closer genre mixes`() {
        val profile = TasteProfile.build(setOf(action, comedy), emptyList(), emptyList())

        assertEquals(1.0, profile.affinity(listOf(action, comedy)), 1e-9)
        assertTrue(profile.affinity(listOf(action, comedy)) > profile.affinity(listOf(action)))
        assertTrue(profile.affinity(listOf(action)) > profile.affinity(listOf(action, horror)))
        assertEquals(0.0, profile.affinity(listOf(horror)), 1e-9)
    }

    @Test
    fun `match percent hides empty profiles and unrelated films`() {
        val profile = TasteProfile.build(setOf(action, comedy), emptyList(), emptyList())

        assertEquals(null, TasteProfile.EMPTY.matchPercent(listOf(action)))
        assertEquals(null, profile.matchPercent(listOf(horror)))
        assertEquals(99, profile.matchPercent(listOf(action, comedy)))
        assertEquals(71, profile.matchPercent(listOf(action)))
    }

    @Test
    fun `dominant genre is the one the user weights most`() {
        val profile = TasteProfile.build(
            selectedGenreIds = setOf(action),
            feedback = listOf(Feedback(movie(1, listOf(comedy)), FeedbackType.LIKE, 1)),
            watchlist = emptyList(),
        )

        assertEquals(comedy, profile.dominantGenre(listOf(action, comedy)))
    }
}
