package com.myth.playpick.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.ThemeMode
import com.myth.playpick.domain.model.UserPreferences
import com.myth.playpick.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val platforms = stringSetPreferencesKey("platforms")
        val genres = stringSetPreferencesKey("genres")
        val tags = stringSetPreferencesKey("tags")
        val theme = stringPreferencesKey("theme")
        val recentSearches = stringPreferencesKey("recent_searches")
        val feedPlatform = stringPreferencesKey("feed_platform")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            UserPreferences(
                onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
                platforms = prefs[Keys.platforms].orEmpty().mapNotNull { enumOrNull<Platform>(it) }.toSet(),
                genreIds = prefs[Keys.genres].orEmpty().mapNotNull(String::toIntOrNull).toSet(),
                tagIds = prefs[Keys.tags].orEmpty().mapNotNull(String::toIntOrNull).toSet(),
                themeMode = prefs[Keys.theme]?.let { enumOrNull<ThemeMode>(it) } ?: ThemeMode.DARK,
                recentSearches = prefs[Keys.recentSearches]?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty(),
                feedPlatform = prefs[Keys.feedPlatform]?.let { enumOrNull<Platform>(it) },
            )
        }

    override suspend fun completeOnboarding(platforms: Set<Platform>, genreIds: Set<Int>, tagIds: Set<Int>) {
        dataStore.edit {
            it[Keys.platforms] = platforms.map(Platform::name).toSet()
            it[Keys.genres] = genreIds.map(Int::toString).toSet()
            it[Keys.tags] = tagIds.map(Int::toString).toSet()
            it[Keys.onboardingComplete] = true
        }
    }

    override suspend fun resetOnboarding() {
        dataStore.edit {
            it.remove(Keys.genres)
            it.remove(Keys.tags)
            it.remove(Keys.feedPlatform)
            it[Keys.onboardingComplete] = false
        }
    }

    override suspend fun setPlatforms(platforms: Set<Platform>) {
        dataStore.edit { prefs ->
            prefs[Keys.platforms] = platforms.map(Platform::name).toSet()
            // A feed narrowed to a platform the user no longer has would be empty.
            val narrowed = prefs[Keys.feedPlatform]?.let { enumOrNull<Platform>(it) }
            if (narrowed != null && narrowed !in platforms) prefs.remove(Keys.feedPlatform)
        }
    }

    override suspend fun setFeedPlatform(platform: Platform?) {
        dataStore.edit { if (platform == null) it.remove(Keys.feedPlatform) else it[Keys.feedPlatform] = platform.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val existing = prefs[Keys.recentSearches]?.split(SEPARATOR).orEmpty()
            prefs[Keys.recentSearches] = (listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) })
                .filter(String::isNotBlank)
                .take(MAX_RECENT_SEARCHES)
                .joinToString(SEPARATOR)
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(Keys.recentSearches) }
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()

    private companion object {
        const val SEPARATOR = "\u001F"
        const val MAX_RECENT_SEARCHES = 8
    }
}
