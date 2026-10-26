package com.myth.movieapp.data.repository

import com.myth.movieapp.data.local.FeedCacheDao
import com.myth.movieapp.data.local.FeedCacheEntity
import com.myth.movieapp.data.local.FeedbackDao
import com.myth.movieapp.data.local.FeedbackEntity
import com.myth.movieapp.data.local.WatchlistDao
import com.myth.movieapp.data.local.WatchlistEntity
import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val dao: FeedbackDao,
    private val clock: Clock,
) : FeedbackRepository {
    override val feedback: Flow<List<Feedback>> = dao.observeAll().map { rows ->
        rows.map { Feedback(it.movie, it.type, it.timestampMillis) }
    }

    override fun feedbackFor(movieId: Int): Flow<FeedbackType?> =
        dao.observeType(movieId).distinctUntilChanged()

    override suspend fun setFeedback(movie: Movie, type: FeedbackType) =
        dao.upsert(FeedbackEntity(movie.id, type, clock.millis(), movie))

    override suspend fun clearFeedback(movieId: Int) = dao.delete(movieId)

    override suspend fun clearAll() = dao.deleteAll()
}

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
    private val clock: Clock,
) : WatchlistRepository {
    override val watchlist: Flow<List<Movie>> = dao.observeAll().map { rows -> rows.map { it.movie } }

    override fun isInWatchlist(movieId: Int): Flow<Boolean> =
        dao.observeContains(movieId).distinctUntilChanged()

    override suspend fun add(movie: Movie) = dao.upsert(WatchlistEntity(movie.id, clock.millis(), movie))

    override suspend fun remove(movieId: Int) = dao.delete(movieId)
}

@Singleton
class FeedCacheRepositoryImpl @Inject constructor(
    private val dao: FeedCacheDao,
) : FeedCacheRepository {
    override val cachedFeed: Flow<ForYouFeed?> = dao.observe().map { it?.feed }

    override suspend fun save(feed: ForYouFeed) = dao.upsert(FeedCacheEntity(feed = feed))

    override suspend fun clear() = dao.clear()
}
