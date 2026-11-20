package com.myth.playpick.data.remote

import com.myth.playpick.data.remote.dto.AchievementDto
import com.myth.playpick.data.remote.dto.GameDetailDto
import com.myth.playpick.data.remote.dto.GameDto
import com.myth.playpick.data.remote.dto.NamedDto
import com.myth.playpick.data.remote.dto.StoreLinkDto
import com.myth.playpick.data.remote.dto.TagDto
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameDetails
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.model.Named
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.StoreLink

fun NamedDto.toDomain() = Named(id, name, slug)

private fun TagDto.toDomain() = Named(id, name, slug)

fun GameDto.toDomain() = Game(
    id = id,
    slug = slug,
    name = name,
    coverUrl = backgroundImage,
    released = released,
    tba = tba,
    rating = rating,
    ratingsCount = ratingsCount,
    metacritic = metacritic,
    playtimeHours = playtime,
    genres = genres.map { it.toDomain() },
    tags = TagPolicy.filter(tags).map { it.toDomain() },
    platforms = parentPlatforms.map { it.platform.toDomain() },
    esrb = esrbRating?.let { it.nameEn ?: it.name }?.takeIf(String::isNotBlank),
    // The first "short screenshot" is the cover again.
    screenshots = shortScreenshots.drop(1).map { it.image },
)

fun GameDetailDto.toGame() = Game(
    id = id,
    slug = slug,
    name = name,
    coverUrl = backgroundImage,
    released = released,
    tba = tba,
    rating = rating,
    ratingsCount = ratingsCount,
    metacritic = metacritic,
    playtimeHours = playtime,
    genres = genres.map { it.toDomain() },
    tags = TagPolicy.filter(tags).map { it.toDomain() },
    platforms = parentPlatforms.map { it.platform.toDomain() },
    esrb = esrbRating?.let { it.nameEn ?: it.name }?.takeIf(String::isNotBlank),
)

/** Store links carry only a store id, so their names come from the detail's own store list. */
fun GameDetailDto.toDomain(storeLinks: List<StoreLinkDto>, screenshots: List<String>): GameDetails {
    val storesById = stores.associate { it.store.id to it.store.toDomain() }
    return GameDetails(
        game = toGame().copy(screenshots = screenshots),
        description = cleanDescription(descriptionRaw),
        developers = developers.map { it.toDomain() },
        publishers = publishers.map { it.toDomain() },
        website = website?.takeIf(String::isNotBlank),
        stores = storeLinks.mapNotNull { link ->
            val store = storesById[link.storeId] ?: return@mapNotNull null
            link.url.takeIf(String::isNotBlank)?.let { StoreLink(store, it) }
        },
        screenshots = screenshots,
        achievementCount = parentAchievementsCount,
        seriesCount = gameSeriesCount,
        additionsCount = additionsCount,
    )
}

/** RAWG descriptions carry Markdown headings ("###Setting"); headings become plain paragraphs. */
internal fun cleanDescription(raw: String): String = raw.lines()
    .map { it.trim().trimStart('#').trim() }
    .joinToString("\n")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

fun AchievementDto.toDomain() = Achievement(
    id = id,
    name = name.trim(),
    description = description.trim(),
    iconUrl = image,
    percent = percent?.toDoubleOrNull(),
)

/** The `games` query string. Every filter is optional; RAWG treats comma lists as "any of". */
fun GameQuery.toParams(): Map<String, String> = buildMap {
    search?.takeIf(String::isNotBlank)?.let { put("search", it) }
    if (genreIds.isNotEmpty()) put("genres", genreIds.sorted().joinToString(","))
    if (tagIds.isNotEmpty()) put("tags", tagIds.sorted().joinToString(","))
    if (developerSlugs.isNotEmpty()) put("developers", developerSlugs.sorted().joinToString(","))
    if (platforms.isNotEmpty()) put("parent_platforms", Platform.rawgIds(platforms).joinToString(","))
    ordering?.let { put("ordering", it.param) }
    metacritic?.let { put("metacritic", "${it.first},${it.last}") }
    released?.let { put("dates", "${it.start},${it.endInclusive}") }
    // DLC, editions and soundtracks would otherwise crowd out the games themselves.
    put("exclude_additions", "true")
    put("page", page.toString())
    put("page_size", pageSize.coerceIn(1, RawgApi.MAX_PAGE_SIZE).toString())
}
