package com.myth.movieapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Thrown before any request is made when no TMDB token was configured at build time. */
class MissingTmdbTokenException : IOException("TMDB_ACCESS_TOKEN is not set")

/** Sends the TMDB v4 read access token as a bearer header, so it never appears in URLs or logs. */
class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (token.isBlank()) throw MissingTmdbTokenException()
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
