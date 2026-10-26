package com.myth.movieapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myth.movieapp.data.local.AppDatabase
import com.myth.movieapp.data.repository.FeedCacheRepositoryImpl
import com.myth.movieapp.data.repository.FeedbackRepositoryImpl
import com.myth.movieapp.data.repository.WatchlistRepositoryImpl
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.recommend.Recommendation
import com.myth.movieapp.domain.recommend.RecommendationReason
import com.myth.movieapp.testing.movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class LocalRepositoriesTest {

    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC)

    @After fun close() = db.close()

    @Test
    fun feedbackRoundTripsMovieAndLatestSignalWins() = runTest {
        val repo = FeedbackRepositoryImpl(db.feedbackDao(), clock)
        val arrival = movie(1, listOf(878), title = "Arrival")

        repo.setFeedback(arrival, FeedbackType.LIKE)
        repo.setFeedback(arrival, FeedbackType.DISLIKE)

        val stored = repo.feedback.first().single()
        assertEquals(arrival, stored.movie)
        assertEquals(FeedbackType.DISLIKE, stored.type)
        assertEquals(FeedbackType.DISLIKE, repo.feedbackFor(1).first())

        repo.clearFeedback(1)
        assertNull(repo.feedbackFor(1).first())
    }

    @Test
    fun watchlistAddsAndRemoves() = runTest {
        val repo = WatchlistRepositoryImpl(db.watchlistDao(), clock)

        repo.add(movie(1))
        assertTrue(repo.isInWatchlist(1).first())
        assertEquals(listOf(1), repo.watchlist.first().map { it.id })

        repo.remove(1)
        assertFalse(repo.isInWatchlist(1).first())
    }

    @Test
    fun feedCacheStoresSealedReasons() = runTest {
        val repo = FeedCacheRepositoryImpl(db.feedCacheDao())
        val feed = ForYouFeed(
            topPicks = listOf(
                Recommendation(movie(1), 0.9, RecommendationReason.BecauseYouLiked(7, "Heat")),
                Recommendation(movie(2), 0.8, RecommendationReason.MatchesGenres(listOf("Drama"))),
                Recommendation(movie(3), 0.7, RecommendationReason.Trending),
            ),
            becauseYouLiked = emptyList(),
            genreRows = emptyList(),
            trending = listOf(movie(4)),
            generatedAtMillis = 42,
        )

        repo.save(feed)

        assertEquals(feed, repo.cachedFeed.first())
    }
}
