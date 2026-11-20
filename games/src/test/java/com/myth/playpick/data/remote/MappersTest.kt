package com.myth.playpick.data.remote

import com.myth.playpick.data.remote.dto.AchievementDto
import com.myth.playpick.data.remote.dto.GameDetailDto
import com.myth.playpick.data.remote.dto.GameDto
import com.myth.playpick.data.remote.dto.PageDto
import com.myth.playpick.data.remote.dto.StoreLinkDto
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Platform
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Fixtures are real RAWG responses (trimmed, key scrubbed) recorded when the endpoints were checked. */
class MappersTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private inline fun <reified T> fixture(name: String): T =
        json.decodeFromString(javaClass.classLoader!!.getResource("rawg/$name.json")!!.readText())

    @Test
    fun `list games keep only English taste tags`() {
        val hollowKnight = fixture<PageDto<GameDto>>("games_list").results.first().toDomain()

        assertEquals(9767, hollowKnight.id)
        assertEquals("Hollow Knight", hollowKnight.name)
        assertEquals(
            listOf("Atmospheric", "Story Rich", "Open World", "2D", "Exploration", "Difficult", "Cute", "Dark Fantasy", "Side Scroller", "Multiple Endings"),
            hollowKnight.tags.map { it.name },
        )
        assertTrue(hollowKnight.tags.none { it.slug.startsWith("steam") || it.id == 31 })
    }

    @Test
    fun `list games carry platforms, rating, playtime and screenshots without the cover`() {
        val game = fixture<PageDto<GameDto>>("games_list").results.first().toDomain()

        assertEquals(listOf("PC", "PlayStation", "Xbox", "Apple Macintosh", "Linux", "Nintendo"), game.platforms.map { it.name })
        assertEquals("Everyone 10+", game.esrb)
        assertEquals(6, game.screenshots.size)
        assertEquals("2017", game.releaseYear)
        assertEquals(LocalDate.of(2017, 2, 23), game.releaseLocalDate())
        assertTrue(game.coverUrl!!.startsWith("https://media.rawg.io/media/"))
    }

    @Test
    fun `features keep genres and tags apart`() {
        val game = fixture<PageDto<GameDto>>("games_list").results.first().toDomain()

        assertTrue(GameFeature.genre(4) in game.features) // Action
        assertTrue(GameFeature.tag(49) in game.features) // Difficult
        assertFalse(GameFeature.tag(4) in game.features) // Funny, not Action
    }

    @Test
    fun `details join store links to store names and use the distinct achievement count`() {
        val detail = fixture<GameDetailDto>("detail")
        val links = fixture<PageDto<StoreLinkDto>>("stores").results

        val details = detail.toDomain(links, screenshots = listOf("a.jpg", "b.jpg"))

        assertEquals(listOf("Steam", "GOG", "Nintendo Store", "Xbox Store", "PlayStation Store"), details.stores.map { it.store.name })
        assertEquals("http://store.steampowered.com/app/367520/", details.stores.first().url)
        assertEquals(39, details.achievementCount)
        assertEquals(listOf("Team Cherry"), details.developers.map { it.name })
        assertEquals("http://hollowknight.com", details.website)
        assertEquals(88, details.game.metacritic)
        assertEquals(listOf("a.jpg", "b.jpg"), details.game.screenshots)
        assertTrue(details.description.startsWith("Hollow Knight"))
    }

    @Test
    fun `store links for stores the detail doesn't list are dropped`() {
        val details = fixture<GameDetailDto>("detail").toDomain(listOf(StoreLinkDto(storeId = 999, url = "https://x")), emptyList())

        assertTrue(details.stores.isEmpty())
    }

    @Test
    fun `achievement percent strings parse, and bad ones become unknown`() {
        val page = fixture<PageDto<com.myth.playpick.data.remote.dto.AchievementDto>>("achievements_page1")
        val first = page.results.first().toDomain()

        assertEquals(1072652L, first.id)
        assertEquals("Heart of Hallownest", first.name)
        assertEquals(1.46, first.percent!!, 1e-9)
        assertNull(AchievementDto(1, "x", percent = "n/a").toDomain().percent)
    }

    @Test
    fun `queries become RAWG parameters, leaving out empty filters`() {
        val params = GameQuery(
            genreIds = setOf(51, 4),
            tagIds = setOf(259),
            platforms = setOf(Platform.NINTENDO, Platform.MOBILE),
            ordering = GameQuery.Ordering.RATING,
            metacritic = 75..100,
            released = LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 11, 1),
            pageSize = 200,
        ).toParams()

        assertEquals("4,51", params["genres"])
        assertEquals("259", params["tags"])
        assertEquals("4,7,8", params["parent_platforms"])
        assertEquals("-rating", params["ordering"])
        assertEquals("75,100", params["metacritic"])
        assertEquals("2026-08-01,2026-11-01", params["dates"])
        assertEquals("40", params["page_size"])
        assertEquals("true", params["exclude_additions"])
        assertFalse("search" in params || "developers" in params)
    }
}
