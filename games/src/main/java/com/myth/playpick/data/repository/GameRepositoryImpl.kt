package com.myth.playpick.data.repository

import com.myth.playpick.data.remote.RawgApi
import com.myth.playpick.data.remote.TagPolicy
import com.myth.playpick.data.remote.toDomain
import com.myth.playpick.data.remote.toParams
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameDetails
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Paged
import com.myth.playpick.domain.repository.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Like [runCatching], but never swallows coroutine cancellation. */
internal inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

@Singleton
class GameRepositoryImpl @Inject constructor(private val api: RawgApi) : GameRepository {

    // Genres and tags barely change; one fetch per process is plenty.
    private val lookupLock = Mutex()
    private var genreCache: List<Named>? = null
    private var tagCache: List<Named>? = null

    override suspend fun games(query: GameQuery): Result<Paged<Game>> = safeCall {
        val page = api.games(query.toParams())
        Paged(page.results.map { it.toDomain() }, query.page, hasMore = page.next != null)
    }

    override suspend fun details(id: Int): Result<GameDetails> = safeCall {
        coroutineScope {
            val detail = async { api.game(id) }
            // Stores and screenshots are extras: the page still works without them.
            val stores = async { safeCall { api.stores(id).results }.getOrDefault(emptyList()) }
            val screenshots = async { safeCall { api.screenshots(id).results.map { it.image } }.getOrDefault(emptyList()) }
            detail.await().toDomain(stores.await(), screenshots.await())
        }
    }

    override suspend fun series(id: Int): Result<List<Game>> = safeCall { api.series(id).results.map { it.toDomain() } }

    override suspend fun additions(id: Int): Result<List<Game>> = safeCall { api.additions(id).results.map { it.toDomain() } }

    override suspend fun achievements(id: Int): Result<List<Achievement>> = safeCall {
        val all = mutableListOf<Achievement>()
        var page = 1
        do {
            val response = api.achievements(id, page)
            all += response.results.map { it.toDomain() }
            page++
        } while (response.next != null && page <= MAX_ACHIEVEMENT_PAGES)
        all.distinctBy { it.id }
    }

    override suspend fun genres(): Result<List<Named>> = lookupLock.withLock {
        genreCache?.let { return Result.success(it) }
        safeCall { api.genres().results.map { it.toDomain() } }.onSuccess { genreCache = it }
    }

    override suspend fun tags(): Result<List<Named>> = lookupLock.withLock {
        tagCache?.let { return Result.success(it) }
        safeCall {
            api.tags().results.filter(TagPolicy::isTasteTag).map { Named(it.id, it.name, it.slug) }
        }.onSuccess { tagCache = it }
    }

    private companion object {
        /** 40 per page; 400 achievements covers even the largest lists without unbounded requests. */
        const val MAX_ACHIEVEMENT_PAGES = 10
    }
}
