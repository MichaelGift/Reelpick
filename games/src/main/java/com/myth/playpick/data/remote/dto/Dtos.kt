package com.myth.playpick.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** RAWG pages every list the same way; `next` is a full URL (including the key), so only its presence is used. */
@Serializable
data class PageDto<T>(
    val count: Int = 0,
    val next: String? = null,
    val results: List<T> = emptyList(),
)

@Serializable
data class NamedDto(val id: Int, val name: String = "", val slug: String = "")

@Serializable
data class TagDto(val id: Int, val name: String = "", val slug: String = "", val language: String? = null)

@Serializable
data class PlatformWrapperDto(val platform: NamedDto)

@Serializable
data class StoreWrapperDto(val store: NamedDto)

@Serializable
data class EsrbDto(val id: Int = 0, val name: String = "", @SerialName("name_en") val nameEn: String? = null)

@Serializable
data class ScreenshotDto(val id: Long = 0, val image: String)

@Serializable
data class GameDto(
    val id: Int,
    val slug: String = "",
    val name: String = "",
    val released: String? = null,
    val tba: Boolean = false,
    @SerialName("background_image") val backgroundImage: String? = null,
    val rating: Double = 0.0,
    @SerialName("ratings_count") val ratingsCount: Int = 0,
    val metacritic: Int? = null,
    val playtime: Int = 0,
    val genres: List<NamedDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    @SerialName("parent_platforms") val parentPlatforms: List<PlatformWrapperDto> = emptyList(),
    @SerialName("esrb_rating") val esrbRating: EsrbDto? = null,
    @SerialName("short_screenshots") val shortScreenshots: List<ScreenshotDto> = emptyList(),
)

@Serializable
data class GameDetailDto(
    val id: Int,
    val slug: String = "",
    val name: String = "",
    val released: String? = null,
    val tba: Boolean = false,
    @SerialName("background_image") val backgroundImage: String? = null,
    val rating: Double = 0.0,
    @SerialName("ratings_count") val ratingsCount: Int = 0,
    val metacritic: Int? = null,
    val playtime: Int = 0,
    val genres: List<NamedDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    @SerialName("parent_platforms") val parentPlatforms: List<PlatformWrapperDto> = emptyList(),
    @SerialName("esrb_rating") val esrbRating: EsrbDto? = null,
    @SerialName("description_raw") val descriptionRaw: String = "",
    val developers: List<NamedDto> = emptyList(),
    val publishers: List<NamedDto> = emptyList(),
    val website: String? = null,
    val stores: List<StoreWrapperDto> = emptyList(),
    /** Distinct achievements; `achievements_count` sums every platform's copy of the same list. */
    @SerialName("parent_achievements_count") val parentAchievementsCount: Int = 0,
    @SerialName("game_series_count") val gameSeriesCount: Int = 0,
    @SerialName("additions_count") val additionsCount: Int = 0,
)

/** One store listing for a game; the store's name comes from the game detail's `stores`. */
@Serializable
data class StoreLinkDto(@SerialName("store_id") val storeId: Int, val url: String = "")

@Serializable
data class AchievementDto(
    val id: Long,
    val name: String = "",
    val description: String = "",
    val image: String? = null,
    /** A string such as "1.46". */
    val percent: String? = null,
)
