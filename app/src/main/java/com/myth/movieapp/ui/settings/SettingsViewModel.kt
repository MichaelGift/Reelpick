package com.myth.movieapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.domain.model.UserPreferences
import com.myth.movieapp.domain.recommend.GetForYouFeedUseCase
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val feedback: FeedbackRepository,
    private val feedCache: FeedCacheRepository,
    private val forYouFeed: GetForYouFeedUseCase,
) : ViewModel() {

    val state: StateFlow<UserPreferences> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences(onboardingComplete = true))

    fun setRegion(region: String?) {
        viewModelScope.launch { preferences.setRegion(region) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    /** Forgets every rating and genre pick; the app returns to onboarding. The watchlist is kept. */
    fun resetTasteProfile() {
        viewModelScope.launch {
            feedback.clearAll()
            feedCache.clear()
            forYouFeed.invalidate()
            preferences.resetOnboarding()
        }
    }
}
