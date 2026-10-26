package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.testing.FakeFeedCacheRepository
import com.myth.movieapp.testing.FakeFeedbackRepository
import com.myth.movieapp.testing.FakeMovieRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.FakeWatchlistRepository
import com.myth.movieapp.testing.movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GetForYouFeedUseCaseTest {

    private val sciFi = 878
    private val comedy = 35

    private val movies = FakeMovieRepository().apply {
        genres = listOf(Genre(sciFi, "Science Fiction"), Genre(comedy, "Comedy"))
    }
    private val feedback = FakeFeedbackRepository()
    private val watchlist = FakeWatchlistRepository()
    private val prefs = FakeUserPreferencesRepository(
        UserPreferences(onboardingComplete = true, selectedGenreIds = setOf(sciFi)),
    )
    private val cache = FakeFeedCacheRepository()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    private val useCase = GetForYouFeedUseCase(
        movies, feedback, watchlist, prefs, cache, CandidateSource(movies), RecommendationRanker(), clock,
    )

    private val arrival = movie(100, listOf(sciFi), title = "Arrival")

    @Test
    fun `builds sections from liked seeds and top genres and caches the feed`() = runTest {
        feedback.setFeedback(arrival, FeedbackType.LIKE)
        movies.recommendations[100] = Result.success(listOf(movie(1, listOf(sciFi)), movie(2, listOf(sciFi))))
        movies.discover[sciFi] = Result.success(listOf(movie(3, listOf(sciFi)), movie(1, listOf(sciFi))))
        movies.trending = Result.success(listOf(movie(4, listOf(comedy)), arrival))

        val feed = useCase().getOrThrow()

        assertEquals(setOf(1, 2, 3, 4), feed.topPicks.map { it.movie.id }.toSet())
        assertEquals(arrival, feed.becauseYouLiked.single().seed)
        assertEquals(setOf(1, 2), feed.becauseYouLiked.single().items.map { it.movie.id }.toSet())
        assertEquals("Science Fiction", feed.genreRows.single().genre.name)
        // The liked seed itself is never recommended back, even when it is trending.
        assertEquals(listOf(4), feed.trending.map { it.id })
        assertEquals(feed, cache.cachedFeed.first())

        // Explainability: matches and the learned profile travel with the feed.
        assertTrue(feed.topPicks.filter { sciFi in it.movie.genreIds }.all { (it.match ?: 0) > 90 })
        assertEquals(listOf("Science Fiction"), feed.taste!!.topGenres.map { it.name })
        assertEquals(1, feed.taste!!.likes)
        assertEquals("Comedy", feed.genreNames[comedy])
    }

    @Test
    fun `dislike re-ranks from the cached pool without refetching`() = runTest {
        feedback.setFeedback(arrival, FeedbackType.LIKE)
        movies.recommendations[100] = Result.success(listOf(movie(1, listOf(sciFi)), movie(2, listOf(sciFi))))
        useCase().getOrThrow()
        val fetchesAfterFirstLoad = movies.fetchCount

        feedback.setFeedback(movie(1, listOf(sciFi)), FeedbackType.NOT_INTERESTED)
        val feed = useCase().getOrThrow()

        assertEquals(fetchesAfterFirstLoad, movies.fetchCount)
        assertFalse(feed.topPicks.any { it.movie.id == 1 })
    }

    @Test
    fun `new like or forced refresh fetches fresh candidates`() = runTest {
        useCase().getOrThrow()
        val first = movies.fetchCount

        feedback.setFeedback(arrival, FeedbackType.LIKE)
        useCase().getOrThrow()
        assertEquals(first + 1, movies.fetchCount)

        useCase(forceRefresh = true).getOrThrow()
        assertEquals(first + 2, movies.fetchCount)
    }

    @Test
    fun `watchlisted movies are excluded from recommendations`() = runTest {
        movies.trending = Result.success(listOf(movie(1, listOf(sciFi)), movie(2, listOf(sciFi))))
        watchlist.add(movie(1, listOf(sciFi)))

        val feed = useCase().getOrThrow()

        assertEquals(listOf(2), feed.topPicks.map { it.movie.id })
    }

    @Test
    fun `fails only when every candidate source fails`() = runTest {
        feedback.setFeedback(arrival, FeedbackType.LIKE)
        movies.recommendations[100] = Result.failure(IOException("offline"))
        movies.discover[sciFi] = Result.failure(IOException("offline"))
        movies.trending = Result.failure(IOException("offline"))

        assertTrue(useCase().isFailure)

        movies.discover[sciFi] = Result.success(listOf(movie(5, listOf(sciFi))))
        assertEquals(listOf(5), useCase(forceRefresh = true).getOrThrow().topPicks.map { it.movie.id })
    }
}
