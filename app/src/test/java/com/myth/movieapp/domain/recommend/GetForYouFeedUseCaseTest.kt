package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.FeedScope
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.testing.FakeFeedCacheRepository
import com.myth.movieapp.testing.FakeFeedbackRepository
import com.myth.movieapp.testing.FakeTitleRepository
import com.myth.movieapp.testing.FakeUserPreferencesRepository
import com.myth.movieapp.testing.FakeWatchlistRepository
import com.myth.movieapp.testing.movie
import com.myth.movieapp.testing.movieKey
import com.myth.movieapp.testing.series
import com.myth.movieapp.testing.seriesKey
import com.myth.movieapp.testing.tasteProfileUseCase
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

    private val movies = FakeTitleRepository().apply {
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
        movies, tasteProfileUseCase(feedback, watchlist, prefs), cache, CandidateSource(movies), RecommendationRanker(), clock,
    )

    private val arrival = movie(100, listOf(sciFi), title = "Arrival")

    @Test
    fun `builds sections from liked seeds and top genres and caches the feed`() = runTest {
        feedback.setFeedback(arrival, FeedbackType.LIKE)
        movies.recommendations[movieKey(100)] = Result.success(listOf(movie(1, listOf(sciFi)), movie(2, listOf(sciFi))))
        movies.discover[sciFi] = Result.success(listOf(movie(3, listOf(sciFi)), movie(1, listOf(sciFi))))
        movies.trending = Result.success(listOf(movie(4, listOf(comedy)), arrival))

        val feed = useCase().getOrThrow()

        assertEquals(setOf(1, 2, 3, 4), feed.topPicks.map { it.movie.id }.toSet())
        assertEquals(arrival, feed.becauseYouLiked.single().seed)
        assertEquals(setOf(1, 2), feed.becauseYouLiked.single().items.map { it.movie.id }.toSet())
        assertEquals("Science Fiction", feed.genreRows.single().genre.name)
        // The liked seed itself is never recommended back, even when it is trending.
        assertEquals(listOf(4), feed.trending.map { it.id })
        assertEquals(feed, cache.cachedFeed(FeedScope.ALL).first())

        // Explainability: matches and the learned profile travel with the feed.
        assertTrue(feed.topPicks.filter { sciFi in it.movie.genreIds }.all { (it.match ?: 0) > 90 })
        assertEquals(listOf("Science Fiction"), feed.taste!!.topGenres.map { it.name })
        assertEquals(1, feed.taste!!.likes)
        assertEquals("Comedy", feed.genreNames[comedy])
    }

    @Test
    fun `dislike re-ranks from the cached pool without refetching`() = runTest {
        feedback.setFeedback(arrival, FeedbackType.LIKE)
        movies.recommendations[movieKey(100)] = Result.success(listOf(movie(1, listOf(sciFi)), movie(2, listOf(sciFi))))
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
        movies.recommendations[movieKey(100)] = Result.failure(IOException("offline"))
        movies.discover[sciFi] = Result.failure(IOException("offline"))
        movies.trending = Result.failure(IOException("offline"))
        movies.discoverSeries[sciFi] = Result.failure(IOException("offline"))
        movies.trendingSeries = Result.failure(IOException("offline"))

        assertTrue(useCase().isFailure)

        movies.discover[sciFi] = Result.success(listOf(movie(5, listOf(sciFi))))
        assertEquals(listOf(5), useCase(forceRefresh = true).getOrThrow().topPicks.map { it.movie.id })
    }

    @Test
    fun `a movie and a series sharing an id are different titles`() = runTest {
        // Disliking movie 7 must not hide series 7. (A different genre, so sci-fi stays in the profile.)
        feedback.setFeedback(movie(7, listOf(comedy)), FeedbackType.DISLIKE)
        movies.discover[sciFi] = Result.success(listOf(movie(7, listOf(sciFi))))
        movies.discoverSeries[sciFi] = Result.success(listOf(series(7, listOf(sciFi))))

        val picks = useCase().getOrThrow().topPicks.map { it.movie.key }

        assertEquals(listOf(seriesKey(7)), picks)
    }

    @Test
    fun `scopes filter one shared pool without refetching`() = runTest {
        movies.discover[sciFi] = Result.success(listOf(movie(1, listOf(sciFi))))
        movies.discoverSeries[sciFi] = Result.success(listOf(series(2, listOf(sciFi))))
        movies.trending = Result.success(listOf(movie(3)))
        movies.trendingSeries = Result.success(listOf(series(4)))

        val all = useCase(FeedScope.ALL).getOrThrow()
        val fetches = movies.fetchCount
        val films = useCase(FeedScope.MOVIES).getOrThrow()
        val shows = useCase(FeedScope.TV).getOrThrow()

        assertEquals(setOf(movieKey(1), seriesKey(2), movieKey(3), seriesKey(4)), all.topPicks.map { it.movie.key }.toSet())
        assertTrue(films.topPicks.all { it.movie.mediaType == MediaType.MOVIE })
        assertTrue(films.trending.all { it.mediaType == MediaType.MOVIE })
        assertTrue(shows.topPicks.all { it.movie.mediaType == MediaType.TV })
        assertEquals(listOf(seriesKey(4)), shows.trending.map { it.key })
        assertEquals(fetches, movies.fetchCount)
        // Each scope is cached on its own.
        assertEquals(shows, cache.cachedFeed(FeedScope.TV).first())
        assertEquals(films, cache.cachedFeed(FeedScope.MOVIES).first())
    }

    @Test
    fun `a liked series seeds series recommendations`() = runTest {
        val dark = series(70523, listOf(sciFi), title = "Dark")
        feedback.setFeedback(dark, FeedbackType.LIKE)
        movies.recommendations[seriesKey(70523)] = Result.success(listOf(series(1, listOf(sciFi)), series(2, listOf(sciFi))))

        val feed = useCase(FeedScope.TV).getOrThrow()

        val row = feed.becauseYouLiked.single()
        assertEquals("Dark", row.seed.title)
        assertEquals(setOf(seriesKey(1), seriesKey(2)), row.items.map { it.movie.key }.toSet())
        assertEquals(
            RecommendationReason.BecauseYouLiked(70523, "Dark"),
            feed.topPicks.first { it.movie.key == seriesKey(1) }.reason,
        )
    }

    @Test
    fun `taste profile can leave out the title being viewed`() = runTest {
        val comedyFilm = movie(9, listOf(comedy))
        feedback.setFeedback(comedyFilm, FeedbackType.LIKE)
        val tasteProfile = tasteProfileUseCase(feedback, watchlist, prefs)

        assertTrue(tasteProfile().weightOf(comedy) > 0)
        assertEquals(0.0, tasteProfile(excluding = comedyFilm.key).weightOf(comedy), 0.0)
    }
}
