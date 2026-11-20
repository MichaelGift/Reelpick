package com.myth.movieapp.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.domain.model.PersonCredit
import com.myth.movieapp.domain.recommend.GetTasteProfileUseCase
import com.myth.movieapp.domain.recommend.TasteProfile
import com.myth.movieapp.domain.repository.TitleRepository
import com.myth.movieapp.ui.common.toUserMessage
import com.myth.movieapp.ui.navigation.PersonRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/** What the previous screen already knew, so the header shows before the profile loads. */
data class PersonPreview(val name: String, val profilePath: String?)

/** A filmography entry with the user's taste match for it (null when there's no overlap). */
data class CreditRow(val credit: PersonCredit, val match: Int?)

/** All of one department's credits, newest first. */
data class Department(val name: String, val credits: List<CreditRow>)

sealed interface PersonContent {
    data object Loading : PersonContent
    data class Error(val message: String) : PersonContent
    data class Loaded(
        val person: Person,
        /** Age today, or at death. */
        val age: Int?,
        val knownFor: List<CreditRow>,
        val departments: List<Department>,
    ) : PersonContent
}

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val titles: TitleRepository,
    private val tasteProfile: GetTasteProfileUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PersonRoute>()
    val sharedKey: String? = route.sharedKey
    val preview = PersonPreview(route.name, route.profilePath)

    private val _content = MutableStateFlow<PersonContent>(PersonContent.Loading)
    val content: StateFlow<PersonContent> = _content.asStateFlow()

    init {
        load()
    }

    fun load() {
        _content.value = PersonContent.Loading
        viewModelScope.launch {
            titles.person(route.personId)
                .onSuccess { person -> _content.value = build(person, tasteProfile(), LocalDate.now(clock)) }
                .onFailure { _content.value = PersonContent.Error(it.toUserMessage()) }
        }
    }

    companion object {
        const val KNOWN_FOR = 8

        /** Talk, News and Reality appearances dominate popularity but aren't what anyone is known for. */
        private val appearanceGenres = setOf(10763, 10764, 10767)

        /** Departments in display order; anything else follows alphabetically. */
        private val departmentOrder = listOf("Acting", "Directing", "Writing", "Creator", "Production")

        internal fun build(person: Person, profile: TasteProfile, today: LocalDate): PersonContent.Loaded {
            fun row(credit: PersonCredit) = CreditRow(credit, profile.matchPercent(credit.title.genreIds))

            val knownFor = person.credits
                .filter { credit -> credit.title.genreIds.none { it in appearanceGenres } && credit.title.posterPath != null }
                // Their own department first: a director is known for what they directed.
                .sortedWith(
                    compareByDescending<PersonCredit> { it.department == (person.knownForDepartment ?: "Acting") }
                        .thenByDescending { it.title.popularity },
                )
                .distinctBy { it.title.key }
                .take(KNOWN_FOR)
                .map(::row)

            val departments = person.credits
                .groupBy { it.department }
                .map { (name, credits) ->
                    Department(
                        name = name,
                        credits = credits
                            // Newest first; undated (announced) titles lead, since they're the next ones.
                            .sortedWith(compareByDescending<PersonCredit> { it.title.releaseDate == null }
                                .thenByDescending { it.title.releaseDate })
                            .map(::row),
                    )
                }
                .sortedWith(
                    compareByDescending<Department> { it.name == person.knownForDepartment }
                        .thenBy { departmentOrder.indexOf(it.name).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                        .thenBy { it.name },
                )

            val age = person.birthday?.let { Period.between(it, person.deathday ?: today).years }
            return PersonContent.Loaded(person, age, knownFor, departments)
        }
    }
}
