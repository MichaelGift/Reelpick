package com.myth.core.recommend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RankerTest {

    /** A minimal item type, unrelated to films, to exercise the generic engine. */
    private data class Item(
        val id: Long,
        val tags: List<String>,
        val rating: Double = 7.0,
        val votes: Int = 1_000,
        val released: LocalDate? = LocalDate.of(2020, 1, 1),
    )

    private object Traits : ItemTraits<Item, Long, String> {
        override fun key(item: Item) = item.id
        override fun features(item: Item) = item.tags
        override fun rating(item: Item) = item.rating
        override fun votes(item: Item) = item.votes
        override fun released(item: Item) = item.released
    }

    private val today = LocalDate.of(2026, 11, 20)
    private val ranker = Ranker(Traits, RankerWeights(), compareBy { it.id })

    private fun profile(vararg tags: String) = TasteProfile.Builder<String>().add(tags.toList(), 1.0).build()

    @Test
    fun `taste affinity outranks a slightly better rating`() {
        val ranked = ranker.rank(
            listOf(Candidate<Item, Long>(Item(1, listOf("sports"), rating = 8.0)), Candidate<Item, Long>(Item(2, listOf("rpg"), rating = 7.5))),
            profile("rpg"),
            excluded = emptySet(),
            today = today,
        )

        assertEquals(listOf(2L, 1L), ranked.map { it.item.id })
        assertTrue(ranked.first().match!! > 90)
    }

    @Test
    fun `seed support lifts an item and saturates`() {
        val one = Candidate<Item, Long>(Item(1, listOf("rpg")), setOf(10L))
        val three = Candidate<Item, Long>(Item(2, listOf("rpg")), setOf(10L, 11L, 12L))
        val five = Candidate<Item, Long>(Item(3, listOf("rpg")), setOf(10L, 11L, 12L, 13L, 14L))
        val p = profile("rpg")

        assertTrue(ranker.score(three, p, today) > ranker.score(one, p, today))
        assertEquals(ranker.score(three, p, today), ranker.score(five.copy(item = five.item.copy(id = 2)), p, today), 1e-9)
    }

    @Test
    fun `excluded and duplicate keys are dropped`() {
        val ranked = ranker.rank(
            listOf(Candidate<Item, Long>(Item(1, listOf("rpg"))), Candidate<Item, Long>(Item(2, listOf("rpg"))), Candidate<Item, Long>(Item(2, listOf("rpg")))),
            profile("rpg"),
            excluded = setOf(1L),
            today = today,
        )

        assertEquals(listOf(2L), ranked.map { it.item.id })
    }

    @Test
    fun `bayesian rating pulls sparse averages toward the prior`() {
        assertTrue(ranker.bayesianRating(Item(1, emptyList(), rating = 8.4, votes = 20_000)) > ranker.bayesianRating(Item(2, emptyList(), rating = 9.0, votes = 12)))
        assertEquals(6.5, ranker.bayesianRating(Item(3, emptyList(), rating = 0.0, votes = 0)), 1e-9)
    }

    @Test
    fun `freshness decays over ten years`() {
        assertEquals(1.0, ranker.freshness(Item(1, emptyList(), released = today.plusDays(3)), today), 1e-9)
        assertEquals(0.0, ranker.freshness(Item(2, emptyList(), released = null), today), 1e-9)
        assertEquals(0.0, ranker.freshness(Item(3, emptyList(), released = LocalDate.of(2001, 1, 1)), today), 1e-9)
        assertEquals(0.5, ranker.freshness(Item(4, emptyList(), released = today.minusDays(1826)), today), 0.01)
    }

    @Test
    fun `no more than two picks in a row share a dominant feature`() {
        val p = TasteProfile.Builder<String>().add(listOf("rpg"), 2.0).add(listOf("puzzle"), 1.0).build()
        val ranked = ranker.rank(
            (1L..4L).map { Candidate<Item, Long>(Item(it, listOf("rpg"), rating = 9.0)) } + Candidate<Item, Long>(Item(5, listOf("puzzle"))),
            p,
            excluded = emptySet(),
            today = today,
        )

        assertEquals(listOf(1L, 2L, 5L, 3L, 4L), ranked.map { it.item.id })
    }

    @Test
    fun `diversity can be limited to some features`() {
        val p = TasteProfile.Builder<String>().add(listOf("t:hard"), 3.0).add(listOf("g:rpg"), 2.0).add(listOf("g:puzzle"), 1.0).build()
        val items = (1L..3L).map { Candidate<Item, Long>(Item(it, listOf("t:hard", "g:rpg"), rating = 9.0)) } +
            Candidate<Item, Long>(Item(4, listOf("t:hard", "g:puzzle")))

        val ranked = ranker.rank(items, p, emptySet(), today) { it.startsWith("g:") }

        assertEquals(listOf(1L, 2L, 4L, 3L), ranked.map { it.item.id })
    }
}
