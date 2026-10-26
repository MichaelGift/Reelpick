package com.myth.movieapp.data.repository

import kotlinx.coroutines.CancellationException

/** Like [runCatching], but never swallows coroutine cancellation. */
internal inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
