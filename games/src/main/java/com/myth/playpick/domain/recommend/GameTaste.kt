package com.myth.playpick.domain.recommend

import com.myth.core.recommend.ItemTraits
import com.myth.core.recommend.TasteProfile
import com.myth.playpick.domain.model.Feedback
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameFeature
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

typealias GameTasteProfile = TasteProfile<GameFeature>

/** How much each signal moves a genre or tag. Finishing a game says more than liking it; 100% says most. */
object GameSignals {
    const val ONBOARDING = 1.0
    const val LIKE = 2.0
    const val DISLIKE = -2.0
    const val NOT_INTERESTED = -2.0
    const val BACKLOG = 1.0
    const val PLAYING = 1.0
    const val COMPLETED = 2.0
    const val MASTERED = 3.0
    const val DROPPED = -2.0

    fun of(type: FeedbackType) = when (type) {
        FeedbackType.LIKE -> LIKE
        FeedbackType.DISLIKE -> DISLIKE
        FeedbackType.NOT_INTERESTED -> NOT_INTERESTED
    }

    fun of(status: LibraryStatus) = when (status) {
        LibraryStatus.BACKLOG -> BACKLOG
        LibraryStatus.PLAYING -> PLAYING
        LibraryStatus.COMPLETED -> COMPLETED
        LibraryStatus.MASTERED -> MASTERED
        LibraryStatus.DROPPED -> DROPPED
    }
}

object GameTaste {
    fun build(
        genreIds: Set<Int>,
        tagIds: Set<Int>,
        feedback: List<Feedback>,
        library: List<LibraryEntry>,
    ): GameTasteProfile {
        val builder = TasteProfile.Builder<GameFeature>()
            .add(genreIds.map(GameFeature::genre) + tagIds.map(GameFeature::tag), GameSignals.ONBOARDING)
        feedback.forEach { builder.add(it.game.features, GameSignals.of(it.type)) }
        library.forEach { builder.add(it.game.features, GameSignals.of(it.status)) }
        return builder.build()
    }
}

/** How the shared engine reads a game. RAWG rates out of 5; the engine works out of 10. */
object GameTraits : ItemTraits<Game, Int, GameFeature> {
    override fun key(item: Game) = item.id
    override fun features(item: Game) = item.features
    override fun rating(item: Game) = item.rating * 2
    override fun votes(item: Game) = item.ratingsCount
    override fun released(item: Game) = item.releaseLocalDate()
}

data class TasteSnapshot(
    val profile: GameTasteProfile,
    val feedback: List<Feedback>,
    val library: List<LibraryEntry>,
) {
    /**
     * Games the user loved, most recent first: likes plus anything playing, finished or 100%-ed.
     * They seed "Because you liked" rows.
     */
    val seeds: List<Game>
        get() {
            val positive = feedback.filter { it.type == FeedbackType.LIKE }.map { it.game to it.createdAtMillis } +
                library.filter { it.status in SEED_STATUSES }.map { it.game to it.updatedAtMillis }
            return positive.sortedByDescending { it.second }.map { it.first }.distinctBy { it.id }
        }

    /** Everything the user has already judged or owns: never recommended back to them. */
    val excludedIds: Set<Int> get() = feedback.mapTo(mutableSetOf()) { it.game.id } + library.map { it.game.id }

    private companion object {
        val SEED_STATUSES = setOf(LibraryStatus.PLAYING, LibraryStatus.COMPLETED, LibraryStatus.MASTERED)
    }
}

class GetTasteProfileUseCase @Inject constructor(
    private val feedback: FeedbackRepository,
    private val library: LibraryRepository,
    private val preferences: UserPreferencesRepository,
) {
    /** @param excluding leave one game's own signals out, so saving a game doesn't inflate its own match. */
    suspend fun snapshot(excluding: Int? = null): TasteSnapshot {
        val prefs = preferences.preferences.first()
        val signals = feedback.feedback.first().filterNot { it.game.id == excluding }
        val entries = library.entries.first().filterNot { it.game.id == excluding }
        return TasteSnapshot(GameTaste.build(prefs.genreIds, prefs.tagIds, signals, entries), signals, entries)
    }

    suspend operator fun invoke(excluding: Int? = null): GameTasteProfile = snapshot(excluding).profile
}
