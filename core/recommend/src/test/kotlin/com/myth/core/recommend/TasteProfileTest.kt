package com.myth.core.recommend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Features here are strings, to prove the profile isn't tied to any app's id type. */
class TasteProfileTest {

    @Test
    fun `nothing positive is an empty profile with no match`() {
        val profile = TasteProfile.Builder<String>().add(listOf("horror"), -2.0).build()

        assertTrue(profile.isEmpty)
        assertNull(profile.matchPercent(listOf("horror")))
        assertEquals(emptyList<String>(), profile.top(3))
    }

    @Test
    fun `weights accumulate and normalise to the strongest signal`() {
        val profile = TasteProfile.Builder<String>()
            .add(listOf("open-world", "rpg"), 1.0)
            .add(listOf("rpg"), 2.0)
            .add(listOf("horror"), -1.5)
            .build()

        assertEquals(1.0, profile.weightOf("rpg"), 1e-9)
        assertEquals(1.0 / 3, profile.weightOf("open-world"), 1e-9)
        assertEquals(-0.5, profile.weightOf("horror"), 1e-9)
        assertEquals(listOf("rpg", "open-world"), profile.top(5))
    }

    @Test
    fun `duplicate features in one signal count once`() {
        val profile = TasteProfile.Builder<String>().add(listOf("rpg", "rpg", "indie"), 1.0).build()

        assertEquals(profile.weightOf("indie"), profile.weightOf("rpg"), 1e-9)
    }

    @Test
    fun `top can be narrowed and ties break by natural order`() {
        val profile = TasteProfile.Builder<String>().add(listOf("g:rpg", "t:metroidvania", "g:action"), 1.0).build()

        assertEquals(listOf("g:action", "g:rpg"), profile.top(5) { it.startsWith("g:") })
    }

    @Test
    fun `affinity is cosine similarity and match percent is clamped`() {
        val profile = TasteProfile.Builder<String>().add(listOf("rpg", "indie"), 1.0).build()

        assertEquals(1.0, profile.affinity(listOf("rpg", "indie")), 1e-9)
        assertEquals(99, profile.matchPercent(listOf("rpg", "indie")))
        assertTrue(profile.affinity(listOf("rpg", "sports")) in 0.0..1.0)
        assertNull(profile.matchPercent(listOf("sports")))
        assertEquals(0.0, profile.affinity(emptyList()), 1e-9)
    }

    @Test
    fun `dominant picks the feature the user cares about most`() {
        val profile = TasteProfile.Builder<String>().add(listOf("rpg"), 2.0).add(listOf("indie"), 1.0).build()

        assertEquals("rpg", profile.dominant(listOf("indie", "rpg")))
    }
}
