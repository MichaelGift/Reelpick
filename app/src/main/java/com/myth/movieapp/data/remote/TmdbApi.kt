package com.myth.movieapp.data.remote

import com.myth.movieapp.data.remote.dto.GenresResponse
import com.myth.movieapp.data.remote.dto.MovieDetailsDto
import com.myth.movieapp.data.remote.dto.MovieDto
import com.myth.movieapp.data.remote.dto.MultiResultDto
import com.myth.movieapp.data.remote.dto.PagedResponse
import com.myth.movieapp.data.remote.dto.PersonDto
import com.myth.movieapp.data.remote.dto.TvDetailsDto
import com.myth.movieapp.data.remote.dto.TvDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    // ---- Movies ----

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

    @GET("movie/{id}")
    suspend fun details(
        @Path("id") movieId: Int,
        @Query("append_to_response") append: String = "credits,videos,watch/providers",
    ): MovieDetailsDto

    // ---- TV ----

    @GET("genre/tv/list")
    suspend fun tvGenres(): GenresResponse

    @GET("tv/popular")
    suspend fun tvPopular(@Query("page") page: Int): PagedResponse<TvDto>

    @GET("tv/top_rated")
    suspend fun tvTopRated(@Query("page") page: Int): PagedResponse<TvDto>

    @GET("trending/tv/week")
    suspend fun tvTrending(): PagedResponse<TvDto>

    @GET("tv/{id}/recommendations")
    suspend fun tvRecommendations(@Path("id") seriesId: Int): PagedResponse<TvDto>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("with_genres") genreIds: String,
        @Query("page") page: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        // Series accumulate votes more slowly than films; 100 keeps quality without starving genres.
        @Query("vote_count.gte") minVotes: Int = 100,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponse<TvDto>

    @GET("tv/{id}")
    suspend fun tvDetails(
        @Path("id") seriesId: Int,
        @Query("append_to_response") append: String = "aggregate_credits,videos,watch/providers",
    ): TvDetailsDto

    // ---- People and search ----

    @GET("person/{id}")
    suspend fun person(
        @Path("id") personId: Int,
        @Query("append_to_response") append: String = "combined_credits",
    ): PersonDto

    /** Movies, series and people in one relevance-ordered list. */
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponse<MultiResultDto>
}
