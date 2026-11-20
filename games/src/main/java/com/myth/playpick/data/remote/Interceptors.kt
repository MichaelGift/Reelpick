package com.myth.playpick.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Thrown before any request is made when no RAWG key was configured at build time. */
class MissingRawgKeyException : IOException("RAWG_API_KEY is not set")

/** RAWG only accepts its key as a query parameter, so it's added here and redacted from logs. */
class KeyInterceptor(private val key: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (key.isBlank()) throw MissingRawgKeyException()
        val request = chain.request()
        val url = request.url.newBuilder().setQueryParameter("key", key).build()
        return chain.proceed(request.newBuilder().url(url).header("Accept", "application/json").build())
    }

    companion object {
        private val keyParam = Regex("key=[^&\\s]+")

        /** For log lines: RAWG keys must never land in logcat. */
        fun redact(line: String): String = line.replace(keyParam, "key=REDACTED")
    }
}

/**
 * RAWG answers with `Cache-Control: no-cache`, and the free tier allows 20,000 requests a month.
 * Successful responses are cached for [maxAgeSeconds] instead; game data changes slowly.
 */
class CacheInterceptor(private val maxAgeSeconds: Int = 6 * 60 * 60) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (chain.request().method != "GET" || !response.isSuccessful) return response
        return response.newBuilder()
            .removeHeader("Pragma")
            .removeHeader("Expires")
            .header("Cache-Control", "public, max-age=$maxAgeSeconds")
            .build()
    }
}
