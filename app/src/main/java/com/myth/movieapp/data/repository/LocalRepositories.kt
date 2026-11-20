package com.myth.movieapp.data.repository

import com.myth.movieapp.data.local.FeedCacheDao
import com.myth.movieapp.data.local.FeedCacheEntity
import com.myth.movieapp.data.local.FeedbackDao
import com.myth.movieapp.data.local.FeedbackEntity
import com.myth.movieapp.data.local.WatchlistDao
import com.myth.movieapp.data.local.WatchlistEntity
import com.myth.movieapp.domain.model.Feedback
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleKey
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val dao: FeedbackDao,
    private val clock: Clock,
) : FeedbackRepository {
    override val feedback: Flow<List<Feedback>> = dao.observeAll().map { rows ->
        rows.map { Feedback(it.title, it.type, it.timestampMillis) }
    }

    override fun feedbackFor(key: TitleKey): Flow<FeedbackType?> =
        dao.observeType(key.type, key.id).distinctUntilChanged()

    override suspend fun setFeedback(title: Title, type: FeedbackType) =
        dao.upsert(FeedbackEntity(title.mediaType, title.id, type, clock.millis(), title))

    override suspend fun clearFeedback(key: TitleKey) = dao.delete(key.type, key.id)

    override suspend fun clearAll() = dao.deleteAll()
}

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
    private val clock: Clock,
) : WatchlistRepository {
    override val watchlist: Flow<List<Title>> = dao.observeAll().map { rows -> rows.map { it.title } }

    override fun isInWatchlist(key: TitleKey): Flow<Boolean> =
        dao.observeContains(key.type, key.id).distinctUntilChanged()

    override suspend fun add(title: Title) =
        dao.upsert(WatchlistEntity(title.mediaType, title.id, clock.millis(), title))

    override suspend fun remove(key: TitleKey) = dao.delete(key.type, key.id)
}

@Singleton
class FeedCacheRepositoryImpl @Inject constructor(
    private val dao: FeedCacheDao,
) : FeedCacheRepository {
    private val json = Json { ignoreUnknownKeys = true }

    // A cache from an older app version may not decode; it's only a cache, so start fresh.
    override val cachedFeed: Flow<ForYouFeed?> = dao.observe().map { row ->
        row?.let { runCatching { json.decodeFromString(ForYouFeed.serializer(), it.feed) }.getOrNull() }
    }

    override suspend fun save(feed: ForYouFeed) =
        dao.upsert(FeedCacheEntity(feed = json.encodeToString(ForYouFeed.serializer(), feed)))

    override suspend fun clear() = dao.clear()
}
