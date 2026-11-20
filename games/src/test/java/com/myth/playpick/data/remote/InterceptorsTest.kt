package com.myth.playpick.data.remote

import com.myth.playpick.data.remote.dto.TagDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterceptorsTest {

    @Test
    fun `keys are redacted from log lines`() {
        val line = "--> GET https://api.rawg.io/api/games?page=1&key=0123abcd&page_size=40"

        assertEquals("--> GET https://api.rawg.io/api/games?page=1&key=REDACTED&page_size=40", KeyInterceptor.redact(line))
    }

    @Test
    fun `tag policy keeps taste and drops storefront, duplicate and non-English tags`() {
        assertTrue(TagPolicy.isTasteTag(TagDto(259, "Metroidvania", "metroidvania", "eng")))
        assertTrue(TagPolicy.isTasteTag(TagDto(18, "Co-op", "co-op", null)))
        assertFalse(TagPolicy.isTasteTag(TagDto(411, "cooperative", "cooperative", "eng")))
        assertFalse(TagPolicy.isTasteTag(TagDto(31, "Singleplayer", "singleplayer", "eng")))
        assertFalse(TagPolicy.isTasteTag(TagDto(99999, "Steam Workshop", "steam-workshop", "eng")))
        assertFalse(TagPolicy.isTasteTag(TagDto(42417, "Экшен", "ekshen", "rus")))
    }

    @Test
    fun `at most ten tags are kept`() {
        val tags = (1..30).map { TagDto(1000 + it, "Tag $it", "tag-$it", "eng") }

        assertEquals(TagPolicy.MAX_TAGS, TagPolicy.filter(tags).size)
    }
}
