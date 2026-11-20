package com.myth.playpick.data.repository

import com.myth.playpick.data.remote.RawgApi
import com.myth.playpick.data.remote.dto.AchievementDto
import com.myth.playpick.data.remote.dto.GameDetailDto
import com.myth.playpick.data.remote.dto.GameDto
import com.myth.playpick.data.remote.dto.NamedDto
import com.myth.playpick.data.remote.dto.PageDto
import com.myth.playpick.data.remote.dto.ScreenshotDto
import com.myth.playpick.data.remote.dto.StoreLinkDto
import com.myth.playpick.data.remote.dto.StoreWrapperDto
import com.myth.playpick.data.remote.dto.TagDto
import com.myth.playpick.domain.model.GameQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GameRepositoryImplTest {

    /** A hand-rolled RAWG: each call is counted, and the store/screenshot calls can be made to fail. */
    private class FakeRawg(
        private val achievementPages: List<List<Long>> = emptyList(),
        private val failExtras: Boolean = false,
    ) : RawgApi {
        val achievementCalls = mutableListOf<Int>()
        var genreCalls = 0
        var lastQuery: Map<String, String> = emptyMap()

        override suspend fun games(query: Map<String, String>): PageDto<GameDto> {
            lastQuery = query
            return PageDto(count = 100, next = "https://next", results = listOf(GameDto(1, name = "One"), GameDto(2, name = "Two")))
        }

        override suspend fun game(id: Int) = GameDetailDto(
            id = id,
            name = "Game $id",
            stores = listOf(StoreWrapperDto(NamedDto(1, "Steam", "steam"))),
            parentAchievementsCount = 3,
        )

        override suspend fun stores(id: Int): PageDto<StoreLinkDto> {
            if (failExtras) throw IOException("down")
            return PageDto(results = listOf(StoreLinkDto(1, "https://store.steampowered.com/app/1")))
        }

        override suspend fun screenshots(id: Int): PageDto<ScreenshotDto> {
            if (failExtras) throw IOException("down")
            return PageDto(results = listOf(ScreenshotDto(1, "shot.jpg")))
        }

        override suspend fun series(id: Int, pageSize: Int) = PageDto<GameDto>()
        override suspend fun additions(id: Int, pageSize: Int) = PageDto<GameDto>()

        override suspend fun achievements(id: Int, page: Int, pageSize: Int): PageDto<AchievementDto> {
            achievementCalls += page
            val ids = achievementPages[page - 1]
            return PageDto(
                count = achievementPages.sumOf { it.size },
                next = if (page < achievementPages.size) "https://next" else null,
                results = ids.map { AchievementDto(it, "A$it", percent = "10.0") },
            )
        }

        override suspend fun genres(): PageDto<NamedDto> {
            genreCalls++
            return PageDto(results = listOf(NamedDto(4, "Action", "action")))
        }

        override suspend fun tags(pageSize: Int) = PageDto(
            results = listOf(TagDto(259, "Metroidvania", "metroidvania", "eng"), TagDto(40847, "Steam Achievements", "steam-achievements", "eng")),
        )
    }

    @Test
    fun `achievements follow every page and drop repeats`() = runTest {
        val api = FakeRawg(achievementPages = listOf(listOf(1L, 2L), listOf(3L, 2L), listOf(4L)))

        val achievements = GameRepositoryImpl(api).achievements(9767).getOrThrow()

        assertEquals(listOf(1, 2, 3), api.achievementCalls)
        assertEquals(listOf(1L, 2L, 3L, 4L), achievements.map { it.id })
    }

    @Test
    fun `details still load when stores and screenshots fail`() = runTest {
        val details = GameRepositoryImpl(FakeRawg(failExtras = true)).details(7).getOrThrow()

        assertEquals("Game 7", details.game.name)
        assertTrue(details.stores.isEmpty())
        assertTrue(details.screenshots.isEmpty())
    }

    @Test
    fun `details join stores and screenshots when available`() = runTest {
        val details = GameRepositoryImpl(FakeRawg()).details(7).getOrThrow()

        assertEquals("Steam", details.stores.single().store.name)
        assertEquals(listOf("shot.jpg"), details.screenshots)
        assertEquals(3, details.achievementCount)
    }

    @Test
    fun `game pages report whether more follow`() = runTest {
        val api = FakeRawg()
        val page = GameRepositoryImpl(api).games(GameQuery(search = "hollow", page = 2)).getOrThrow()

        assertEquals(2, page.page)
        assertTrue(page.hasMore)
        assertEquals("hollow", api.lastQuery["search"])
    }

    @Test
    fun `genres are fetched once and tags lose storefront entries`() = runTest {
        val api = FakeRawg()
        val repo = GameRepositoryImpl(api)

        repo.genres(); repo.genres()
        val tags = repo.tags().getOrThrow()

        assertEquals(1, api.genreCalls)
        assertEquals(listOf("Metroidvania"), tags.map { it.name })
    }

    @Test
    fun `failures come back as results`() = runTest {
        val broken = object : RawgApi by FakeRawg() {
            override suspend fun game(id: Int): GameDetailDto = throw IOException("offline")
        }

        assertFalse(GameRepositoryImpl(broken).details(1).isSuccess)
    }
}
