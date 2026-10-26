package com.myth.movieapp.data.remote

import com.myth.movieapp.data.remote.dto.GenresResponse
import com.myth.movieapp.data.remote.dto.MovieDetailsDto
import com.myth.movieapp.data.remote.dto.MovieDto
import com.myth.movieapp.data.remote.dto.PagedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("genre/movie/list")
    suspend fun genres(): GenresResponse

    @GET("movie/popular")
    suspend fun popular(@Query("page") page: Int): PagedResponse<MovieDto>

    @GET("movie/top_rated")
    suspend fun topRated(@Query("page") page: Int): PagedResponse<MovieDto>

    @GET("trending/movie/week")
    suspend fun trending(): PagedResponse<MovieDto>

    @GET("movie/{id}/recommendations")
    suspend fun recommendations(@Path("id") movieId: Int): PagedResponse<MovieDto>

    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") genreIds: String,
        @Query("page") page: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("vote_count.gte") minVotes: Int = 200,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponse<MovieDto>

    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponse<MovieDto>

    @GET("movie/{id}")
    suspend fun details(
        @Path("id") movieId: Int,
        @Query("append_to_response") append: String = "credits,videos,watch/providers",
    ): MovieDetailsDto
}
