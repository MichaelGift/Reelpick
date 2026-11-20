package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Everything the taste profile was built from, for callers that need the raw signals too. */
data class TasteSnapshot(
    val profile: TasteProfile,
    val feedback: List<Feedback>,
    val watchlist: List<Title>,
)

/** Builds the user's current [TasteProfile] from onboarding genres, ratings and the watchlist. */
class GetTasteProfileUseCase @Inject constructor(
    private val feedback: FeedbackRepository,
    private val watchlist: WatchlistRepository,
    private val preferences: UserPreferencesRepository,
) {
    /**
     * @param excluding leave this title's own signals out, so rating or saving a title doesn't
     *   inflate the match shown for that same title.
     */
    suspend fun snapshot(excluding: TitleKey? = null): TasteSnapshot {
        val genres = preferences.preferences.first().selectedGenreIds
        val signals = feedback.feedback.first().filterNot { it.movie.key == excluding }
        val saved = watchlist.watchlist.first().filterNot { it.key == excluding }
        return TasteSnapshot(TasteProfile.build(genres, signals, saved), signals, saved)
    }

    suspend operator fun invoke(excluding: TitleKey? = null): TasteProfile = snapshot(excluding).profile
}
