package com.myth.playpick.data.remote

import com.myth.playpick.data.remote.dto.AchievementDto
import com.myth.playpick.data.remote.dto.GameDetailDto
import com.myth.playpick.data.remote.dto.GameDto
import com.myth.playpick.data.remote.dto.NamedDto
import com.myth.playpick.data.remote.dto.PageDto
import com.myth.playpick.data.remote.dto.ScreenshotDto
import com.myth.playpick.data.remote.dto.StoreLinkDto
import com.myth.playpick.data.remote.dto.TagDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * The parts of RAWG's free tier Playpick uses. "Similar games" is a paid endpoint, so related games
 * come from series, developers and shared tags instead. The key is added by [KeyInterceptor].
 */
interface RawgApi {
    @GET("games")
    suspend fun games(@QueryMap query: Map<String, String>): PageDto<GameDto>

    @GET("games/{id}")
    suspend fun game(@Path("id") id: Int): GameDetailDto

    @GET("games/{id}/stores")
    suspend fun stores(@Path("id") id: Int): PageDto<StoreLinkDto>

    @GET("games/{id}/screenshots")
    suspend fun screenshots(@Path("id") id: Int): PageDto<ScreenshotDto>

    @GET("games/{id}/game-series")
    suspend fun series(@Path("id") id: Int, @Query("page_size") pageSize: Int = 20): PageDto<GameDto>

    @GET("games/{id}/additions")
    suspend fun additions(@Path("id") id: Int, @Query("page_size") pageSize: Int = 20): PageDto<GameDto>

    @GET("games/{id}/achievements")
    suspend fun achievements(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int = MAX_PAGE_SIZE,
    ): PageDto<AchievementDto>

    @GET("genres")
    suspend fun genres(): PageDto<NamedDto>

    @GET("tags")
    suspend fun tags(@Query("page_size") pageSize: Int = MAX_PAGE_SIZE): PageDto<TagDto>

    companion object {
        /** RAWG caps every list at 40 per page. */
        const val MAX_PAGE_SIZE = 40
    }
}
