package com.myth.movieapp.domain.recommend

import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Title
import com.myth.core.recommend.TasteProfile as CoreTasteProfile

/** Reelpick's taste: a weight per TMDB genre id (TV genres are mapped into the same space). */
typealias TasteProfile = CoreTasteProfile<Int>

/** How much each kind of signal moves a genre's weight. */
object TasteSignals {
    const val SELECTED_GENRE = 1.0
    const val LIKE = 2.0
    const val DISLIKE = -2.0
    const val NOT_INTERESTED = -2.0
    const val WATCHLIST = 1.0
}

val CoreTasteProfile.Companion.EMPTY: TasteProfile get() = empty()

/** Builds the profile from onboarding genres, ratings and the watchlist. */
fun CoreTasteProfile.Companion.build(
    selectedGenreIds: Set<Int>,
    feedback: List<Feedback>,
    watchlist: List<Title>,
): TasteProfile {
    val builder = CoreTasteProfile.Builder<Int>().add(selectedGenreIds, TasteSignals.SELECTED_GENRE)
    feedback.forEach { signal ->
        val delta = when (signal.type) {
            FeedbackType.LIKE -> TasteSignals.LIKE
            FeedbackType.DISLIKE -> TasteSignals.DISLIKE
            FeedbackType.NOT_INTERESTED -> TasteSignals.NOT_INTERESTED
            FeedbackType.WATCHED -> 0.0 // Watching says nothing about whether they enjoyed it.
        }
        builder.add(signal.movie.genreIds, delta)
    }
    watchlist.forEach { builder.add(it.genreIds, TasteSignals.WATCHLIST) }
    return builder.build()
}
