package com.myth.movieapp.ui.person

import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.PersonCredit
import com.myth.movieapp.domain.recommend.TasteProfile
import com.myth.movieapp.domain.recommend.EMPTY
import com.myth.movieapp.domain.recommend.build
import com.myth.movieapp.testing.movie
import com.myth.movieapp.testing.series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonViewModelTest {

    private val sciFi = 878
    private val talk = 10767
    private val today = LocalDate.of(2026, 11, 20)

    private fun person(credits: List<PersonCredit>, department: String = "Directing", born: LocalDate? = null, died: LocalDate? = null) =
        Person(
            id = 1, name = "Someone", biography = "", birthday = born, deathday = died,
            placeOfBirth = null, profilePath = null, knownForDepartment = department, credits = credits,
        )

    private fun poster(title: com.myth.movieapp.domain.model.Title, popularity: Double) =
        title.copy(posterPath = "/p.jpg", popularity = popularity)

    @Test
    fun `known for prefers their own department, skips talk shows and repeats`() {
        val directed = poster(movie(1, listOf(sciFi)), popularity = 10.0)
        val actedInHuge = poster(movie(2, listOf(sciFi)), popularity = 500.0)
        val talkShow = poster(series(3, listOf(talk)), popularity = 900.0)
        val content = PersonViewModel.build(
            person(
                listOf(
                    PersonCredit(actedInHuge, "Cameo", "Acting"),
                    PersonCredit(talkShow, "Himself", "Acting"),
                    PersonCredit(directed, "Director", "Directing"),
                    PersonCredit(directed, "Writer", "Writing"),
                ),
            ),
            TasteProfile.EMPTY,
            today,
        )

        assertEquals(listOf(1, 2), content.knownFor.map { it.credit.title.id })
    }

    @Test
    fun `departments lead with theirs and list undated then newest first`() {
        val content = PersonViewModel.build(
            person(
                listOf(
                    PersonCredit(movie(1, releaseDate = "2010-01-01"), null, "Acting"),
                    PersonCredit(movie(2, releaseDate = "2014-01-01"), null, "Directing"),
                    PersonCredit(movie(3, releaseDate = null), null, "Directing"),
                    PersonCredit(movie(4, releaseDate = "2023-01-01"), null, "Directing"),
                    PersonCredit(movie(5, releaseDate = "2001-01-01"), null, "Sound"),
                ),
            ),
            TasteProfile.EMPTY,
            today,
        )

        assertEquals(listOf("Directing", "Acting", "Sound"), content.departments.map { it.name })
        assertEquals(listOf(3, 4, 2), content.departments.first().credits.map { it.credit.title.id })
    }

    @Test
    fun `every credit carries the user's taste match`() {
        val profile = TasteProfile.build(setOf(sciFi), emptyList(), emptyList())
        val content = PersonViewModel.build(
            person(listOf(PersonCredit(movie(1, listOf(sciFi)), null, "Directing"), PersonCredit(movie(2, listOf(35)), null, "Directing"))),
            profile,
            today,
        )

        val matches = content.departments.single().credits.associate { it.credit.title.id to it.match }
        assertTrue(matches.getValue(1)!! > 90)
        assertNull(matches.getValue(2))
    }

    @Test
    fun `age counts to today or to their death`() {
        assertEquals(56, PersonViewModel.build(person(emptyList(), born = LocalDate.of(1970, 7, 30)), TasteProfile.EMPTY, today).age)
        assertEquals(
            82,
            PersonViewModel.build(
                person(emptyList(), born = LocalDate.of(1934, 6, 1), died = LocalDate.of(2016, 12, 1)),
                TasteProfile.EMPTY,
                today,
            ).age,
        )
        assertNull(PersonViewModel.build(person(emptyList()), TasteProfile.EMPTY, today).age)
    }
}
