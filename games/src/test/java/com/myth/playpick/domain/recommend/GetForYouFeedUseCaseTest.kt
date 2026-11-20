package com.myth.playpick.domain.recommend

import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.UserPreferences
import com.myth.playpick.testing.FakeFeedCacheRepository
import com.myth.playpick.testing.FakeFeedbackRepository
import com.myth.playpick.testing.FakeGameRepository
import com.myth.playpick.testing.FakeLibraryRepository
import com.myth.playpick.testing.FakePreferencesRepository
import com.myth.playpick.testing.NINTENDO
import com.myth.playpick.testing.PC
import com.myth.playpick.testing.entry
import com.myth.playpick.testing.game
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GetForYouFeedUseCaseTest {

    private val rpg = 5
    private val metroidvania = 259
    private val hollowKnight = game(9767, genres = listOf(51), tags = listOf(metroidvania), name = "Hollow Knight")

    private val rawg = FakeGameRepository()
    private val feedback = FakeFeedbackRepository(listOf(Feedback(hollowKnight, FeedbackType.LIKE, 1)))
    private val library = FakeLibraryRepository()
    private val prefs = FakePreferencesRepository(UserPreferences(onboardingComplete = true, platforms = setOf(Platform.PC, Platform.NINTENDO), genreIds = setOf(rpg)))
    private val cache = FakeFeedCacheRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC)

    private fun useCase() = GetForYouFeedUseCase(
        GetTasteProfileUseCase(feedback, library, prefs),
        prefs,
        CandidateSource(rawg),
        cache,
        Json { ignoreUnknownKeys = true },
        clock,
    )

    @Test
    fun `seeds lead to their series, developers and tag neighbours, with reasons`() = runTest {
        rawg.seriesById[9767] = listOf(game(1, tags = listOf(metroidvania), name = "Silksong"))
        rawg.developersById[9767] = listOf(Named(3748, "Team Cherry", "team-cherry"))
        rawg.whenQuery({ "team-cherry" in it.developerSlugs }, listOf(game(2, tags = listOf(metroidvania))))
        rawg.whenQuery({ metroidvania in it.tagIds }, listOf(game(3, tags = listOf(metroidvania)), game(1, tags = listOf(metroidvania))))

        val feed = useCase()().getOrThrow()

        val row = feed.becauseYouLiked.single()
        assertEquals("Hollow Knight", row.seed.name)
        assertEquals(setOf(1, 2, 3), row.picks.map { it.game.id }.toSet())
        assertEquals("Because you liked Hollow Knight", feed.topPicks.first().reason.label())
        // Reached from the series, the developer and the tags: the strongest support.
        assertEquals(1, feed.topPicks.first().game.id)
    }

    @Test
    fun `a game reached from two seeds shows only in the newest seed's row`() = runTest {
        val celeste = game(500, tags = listOf(49), name = "Celeste")
        feedback.setFeedback(celeste, FeedbackType.LIKE) // newer than Hollow Knight
        rawg.seriesById[500] = listOf(game(1, tags = listOf(49)))
        rawg.seriesById[9767] = listOf(game(1, tags = listOf(49)), game(2, tags = listOf(metroidvania)))

        val rows = useCase()().getOrThrow().becauseYouLiked

        assertEquals(listOf("Celeste", "Hollow Knight"), rows.map { it.seed.name })
        assertEquals(listOf(1), rows[0].picks.map { it.game.id })
        assertEquals(listOf(2), rows[1].picks.map { it.game.id })
    }

    @Test
    fun `games already judged or owned are never recommended`() = runTest {
        library.setStatus(game(10, genres = listOf(rpg)), LibraryStatus.BACKLOG)
        feedback.setFeedback(game(11, genres = listOf(rpg)), FeedbackType.NOT_INTERESTED)
        // "Not interested" cancels out the onboarding RPG pick, so answer any genre discovery.
        rawg.whenQuery({ it.genreIds.isNotEmpty() }, listOf(game(10, genres = listOf(rpg)), game(11, genres = listOf(rpg)), game(12, genres = listOf(rpg))))

        val ids = useCase()().getOrThrow().topPicks.map { it.game.id }

        assertEquals(listOf(12), ids)
    }

    @Test
    fun `the platform switch filters the same pool without refetching`() = runTest {
        rawg.whenQuery({ rpg in it.genreIds }, listOf(game(20, genres = listOf(rpg), platforms = listOf(PC)), game(21, genres = listOf(rpg), platforms = listOf(NINTENDO))))
        val useCase = useCase()

        val all = useCase().getOrThrow()
        val calls = rawg.queries.size
        val switchOnly = useCase(scope = Platform.NINTENDO).getOrThrow()

        assertEquals(setOf(20, 21), all.topPicks.map { it.game.id }.toSet())
        assertEquals(listOf(21), switchOnly.topPicks.map { it.game.id })
        assertEquals(calls, rawg.queries.size)
        assertTrue(rawg.queries.all { it.platforms == setOf(Platform.PC, Platform.NINTENDO) })
    }

    @Test
    fun `short and sweet keeps strong matches under ten hours`() = runTest {
        // Strong matches share the liked game's genre and tag; a weak one is short but not advertised.
        fun strong(id: Int, hours: Int) = game(id, genres = listOf(51), tags = listOf(metroidvania), playtime = hours)
        rawg.whenQuery({ rpg in it.genreIds }, listOf(strong(30, 6), strong(31, 60), strong(32, 0), game(33, genres = listOf(rpg), playtime = 5)))

        val short = useCase()().getOrThrow().shortAndSweet.map { it.game.id }

        assertEquals(listOf(30), short)
    }

    @Test
    fun `the feed is cached per scope and survives a decode`() = runTest {
        rawg.whenQuery({ rpg in it.genreIds }, listOf(game(40, genres = listOf(rpg))))
        val useCase = useCase()

        useCase(scope = Platform.PC).getOrThrow()

        assertNotNull(useCase.cachedFeed(Platform.PC))
        assertEquals(listOf(40), useCase.cachedFeed(Platform.PC)!!.topPicks.map { it.game.id })
        assertEquals(null, useCase.cachedFeed(null))
    }

    @Test
    fun `the feed fails only when every source fails`() = runTest {
        rawg.failing = true

        assertFalse(useCase()().isSuccess)
    }

    @Test
    fun `the taste summary names genres and tags`() = runTest {
        rawg.whenQuery({ rpg in it.genreIds }, listOf(game(50, genres = listOf(rpg))))

        val taste = useCase()().getOrThrow().taste!!

        assertTrue(taste.tags.any { it.name == "Tag $metroidvania" })
        assertEquals(1, taste.liked)
    }
}
