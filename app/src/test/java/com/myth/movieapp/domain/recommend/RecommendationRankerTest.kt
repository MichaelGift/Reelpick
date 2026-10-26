package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.testing.movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecommendationRankerTest {

    private val action = 28
    private val comedy = 35
    private val drama = 18
    private val today = LocalDate.of(2026, 1, 1)
    private val names = mapOf(action to "Action", comedy to "Comedy", drama to "Drama")
    private val ranker = RecommendationRanker()

    private fun profile(vararg genres: Int) = TasteProfile.build(genres.toSet(), emptyList(), emptyList())

    private fun candidate(id: Int, genres: List<Int>, vararg origins: CandidateOrigin, votes: Int = 1_000) =
        Candidate(movie(id, genres, voteCount = votes), origins.toSet().ifEmpty { setOf(CandidateOrigin.Trending) })

    @Test
    fun `excluded movies and seeds never appear`() {
        val seed = movie(1, listOf(action))
        val result = ranker.rank(
            candidates = listOf(candidate(1, listOf(action)), candidate(2, listOf(action)), candidate(3, listOf(action))),
            profile = profile(action),
            seeds = listOf(seed),
            excludedIds = setOf(2),
            genreNames = names,
            today = today,
        )

        assertEquals(listOf(3), result.map { it.movie.id })
    }

    @Test
    fun `genre affinity and seed support lift a candidate`() {
        val seed = movie(100, listOf(comedy), title = "Superbad")
        val result = ranker.rank(
            candidates = listOf(
                candidate(1, listOf(drama)),
                candidate(2, listOf(comedy)),
                candidate(3, listOf(comedy), CandidateOrigin.Seed(100)),
            ),
            profile = profile(comedy),
            seeds = listOf(seed),
            excludedIds = emptySet(),
            genreNames = names,
            today = today,
        )

        assertEquals(listOf(3, 2, 1), result.map { it.movie.id })
    }

    @Test
    fun `bayesian rating stops a high average from few votes winning`() {
        val obscure = movie(1, voteAverage = 9.5, voteCount = 10)
        val classic = movie(2, voteAverage = 8.4, voteCount = 20_000)

        assertTrue(ranker.bayesianRating(classic) > ranker.bayesianRating(obscure))
        assertEquals(6.5, ranker.bayesianRating(movie(3, voteAverage = 0.0, voteCount = 0)), 1e-9)
    }

    @Test
    fun `freshness decays to zero after ten years and is full for upcoming releases`() {
        assertEquals(1.0, ranker.freshness(movie(1, releaseDate = "2026-06-01"), today), 1e-9)
        assertEquals(0.0, ranker.freshness(movie(2, releaseDate = "1999-01-01"), today), 1e-9)
        assertEquals(0.0, ranker.freshness(movie(3, releaseDate = null), today), 1e-9)
        val fiveYears = ranker.freshness(movie(4, releaseDate = "2021-01-01"), today)
        assertEquals(0.5, fiveYears, 0.01)
    }

    @Test
    fun `reason cites the most recently liked seed that recommended it`() {
        val older = movie(10, title = "Alien")
        val newer = movie(11, title = "Arrival")
        val result = ranker.rank(
            candidates = listOf(candidate(1, listOf(drama), CandidateOrigin.Seed(10), CandidateOrigin.Seed(11))),
            profile = profile(drama),
            seeds = listOf(newer, older),
            excludedIds = emptySet(),
            genreNames = names,
            today = today,
        )

        assertEquals(RecommendationReason.BecauseYouLiked(11, "Arrival"), result.single().reason)
    }

    @Test
    fun `reason falls back to matched genres then trending`() {
        val result = ranker.rank(
            candidates = listOf(candidate(1, listOf(comedy, action)), candidate(2, listOf(drama))),
            profile = TasteProfile.build(
                setOf(action),
                listOf(Feedback(movie(9, listOf(comedy)), FeedbackType.LIKE, 1)),
                emptyList(),
            ),
            seeds = emptyList(),
            excludedIds = emptySet(),
            genreNames = names,
            today = today,
        ).associateBy { it.movie.id }

        assertEquals(RecommendationReason.MatchesGenres(listOf("Comedy", "Action")), result.getValue(1).reason)
        assertEquals(RecommendationReason.Trending, result.getValue(2).reason)
    }

    @Test
    fun `diversity pass breaks up long runs of one genre`() {
        val p = TasteProfile.build(
            setOf(drama),
            listOf(Feedback(movie(9, listOf(action)), FeedbackType.LIKE, 1)),
            emptyList(),
        )
        // Four action movies that would otherwise all outrank the drama.
        val result = ranker.rank(
            candidates = (1..4).map { candidate(it, listOf(action)) } + candidate(5, listOf(drama)),
            profile = p,
            seeds = emptyList(),
            excludedIds = emptySet(),
            genreNames = names,
            today = today,
        ).map { it.movie.id }

        assertEquals(listOf(1, 2, 5, 3, 4), result)
    }

    @Test
    fun `empty profile still ranks by quality`() {
        val result = ranker.rank(
            candidates = listOf(
                Candidate(movie(1, voteAverage = 6.0), setOf(CandidateOrigin.Trending)),
                Candidate(movie(2, voteAverage = 8.0), setOf(CandidateOrigin.Trending)),
            ),
            profile = TasteProfile.EMPTY,
            seeds = emptyList(),
            excludedIds = emptySet(),
            genreNames = names,
            today = today,
        )

        assertEquals(listOf(2, 1), result.map { it.movie.id })
    }
}
