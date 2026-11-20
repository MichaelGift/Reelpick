package com.myth.playpick.domain.model

import com.myth.playpick.ui.common.RawgImages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun `rarity bands follow the share of players`() {
        assertEquals(Rarity.COMMON, Rarity.of(72.0))
        assertEquals(Rarity.UNCOMMON, Rarity.of(20.0))
        assertEquals(Rarity.RARE, Rarity.of(5.0))
        assertEquals(Rarity.ULTRA_RARE, Rarity.of(1.46))
        assertNull(Achievement(1, "x", "y", percent = null).rarity)
    }

    @Test
    fun `progress is complete only when every achievement is ticked`() {
        assertTrue(Progress(39, 39).isComplete)
        assertEquals(0.5f, Progress(1, 2).fraction)
        assertEquals(0f, Progress(0, 0).fraction)
        assertEquals(false, Progress(0, 0).isComplete)
    }

    @Test
    fun `genre and tag features sort genres first`() {
        val sorted = listOf(GameFeature.tag(1), GameFeature.genre(9), GameFeature.genre(2)).sorted()

        assertEquals(listOf(GameFeature.genre(2), GameFeature.genre(9), GameFeature.tag(1)), sorted)
    }

    @Test
    fun `platforms expand to RAWG parent ids`() {
        assertEquals(listOf(1, 4, 8), Platform.rawgIds(setOf(Platform.MOBILE, Platform.PC)))
    }

    @Test
    fun `RAWG covers are resized once`() {
        val url = "https://media.rawg.io/media/games/4cf/4cfc6b7f1850590a4634b08bfab308ab.jpg"

        assertEquals("https://media.rawg.io/media/resize/640/-/games/4cf/4cfc6b7f1850590a4634b08bfab308ab.jpg", RawgImages.cover(url))
        assertEquals(RawgImages.cover(url), RawgImages.resized(RawgImages.cover(url), 200))
        assertNull(RawgImages.cover(null))
    }
}
