package com.myth.movieapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreMapTest {

    @Test
    fun `combined TV genres split into their movie equivalents`() {
        // Action & Adventure, Sci-Fi & Fantasy, War & Politics, Kids
        assertEquals(listOf(28, 12, 878, 14, 10752, 10751), GenreMap.normalizeTv(listOf(10759, 10765, 10768, 10762)))
    }

    @Test
    fun `shared and TV-only genres keep their ids and duplicates collapse`() {
        // Drama (shared), Reality (TV-only), Kids -> Family, Family
        assertEquals(listOf(18, 10764, 10751), GenreMap.normalizeTv(listOf(18, 10764, 10762, 10751)))
    }

    @Test
    fun `profile genres map back to the TV genre to discover with`() {
        assertEquals(10759, GenreMap.tvGenreFor(28))
        assertEquals(10759, GenreMap.tvGenreFor(12))
        assertEquals(10765, GenreMap.tvGenreFor(878))
        assertEquals(18, GenreMap.tvGenreFor(18))
        assertEquals(10764, GenreMap.tvGenreFor(10764))
    }

    @Test
    fun `movie-only genres have no TV equivalent`() {
        assertNull(GenreMap.tvGenreFor(27)) // Horror
        assertNull(GenreMap.tvGenreFor(53)) // Thriller
        assertNull(GenreMap.tvGenreFor(10770)) // TV Movie
    }

    @Test
    fun `only TV genres without a movie form join the shared list`() {
        assertTrue(GenreMap.isTvOnly(10764)) // Reality
        assertTrue(GenreMap.isTvOnly(10767)) // Talk
        assertFalse(GenreMap.isTvOnly(10759)) // expressed as Action + Adventure
        assertFalse(GenreMap.isTvOnly(18)) // already a movie genre
    }
}
