package com.myth.movieapp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.repository.UserPreferencesRepository
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
        val selectedGenres = stringSetPreferencesKey("selected_genres")
        val region = stringPreferencesKey("region")
        val theme = stringPreferencesKey("theme")
        val recentSearches = stringPreferencesKey("recent_searches")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            UserPreferences(
                onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
                selectedGenreIds = prefs[Keys.selectedGenres].orEmpty().mapNotNull(String::toIntOrNull).toSet(),
                region = prefs[Keys.region],
                themeMode = prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.DARK,
                recentSearches = prefs[Keys.recentSearches]?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty(),
            )
        }

    override suspend fun completeOnboarding(genreIds: Set<Int>) {
        dataStore.edit {
            it[Keys.selectedGenres] = genreIds.map(Int::toString).toSet()
            it[Keys.onboardingComplete] = true
        }
    }

    override suspend fun resetOnboarding() {
        dataStore.edit {
            it.remove(Keys.selectedGenres)
            it[Keys.onboardingComplete] = false
        }
    }

    override suspend fun setRegion(region: String?) {
        dataStore.edit { if (region == null) it.remove(Keys.region) else it[Keys.region] = region }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val existing = prefs[Keys.recentSearches]?.split(SEPARATOR).orEmpty()
            val updated = (listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) })
                .filter(String::isNotBlank)
                .take(MAX_RECENT_SEARCHES)
            prefs[Keys.recentSearches] = updated.joinToString(SEPARATOR)
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(Keys.recentSearches) }
    }

    private companion object {
        const val SEPARATOR = "\u001F"
        const val MAX_RECENT_SEARCHES = 8
    }
}
