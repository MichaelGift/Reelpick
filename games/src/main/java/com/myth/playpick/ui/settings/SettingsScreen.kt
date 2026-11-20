package com.myth.playpick.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.myth.core.design.Card
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.model.ThemeMode
import com.myth.playpick.domain.model.UserPreferences
import com.myth.playpick.domain.recommend.GetForYouFeedUseCase
import com.myth.playpick.domain.repository.FeedCacheRepository
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.navigation.FloatingBarClearance
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
    private val feed: GetForYouFeedUseCase,
) : ViewModel() {
    val prefs: StateFlow<UserPreferences> = preferences.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    /** At least one platform must stay selected, or nothing could be recommended. */
    fun togglePlatform(platform: Platform) {
        val current = prefs.value.platforms
        val next = if (platform in current) current - platform else current + platform
        if (next.isEmpty()) return
        viewModelScope.launch {
            preferences.setPlatforms(next)
            feed.invalidate()
            feedCache.clear()
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    /** Forgets likes and onboarding picks; the library and checklists are kept. */
    fun resetTaste() {
        viewModelScope.launch {
            feedback.clear()
            feedCache.clear()
            feed.invalidate()
            preferences.resetOnboarding()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))

        Section("Your platforms", "Recommendations only include games you can play.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { Pill(it.label, it in prefs.platforms) { viewModel.togglePlatform(it) } }
            }
        }
        Section("Theme", null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.DARK to "Dark", ThemeMode.LIGHT to "Light", ThemeMode.SYSTEM to "System").forEach { (mode, label) ->
                    Pill(label, prefs.themeMode == mode) { viewModel.setTheme(mode) }
                }
            }
        }
        Section("Taste profile", "Start over with new genres, tags and favourites. Your library and checklists stay.") {
            OutlinedButton(onClick = { confirmReset = true }) { Text("Reset taste profile") }
        }
        RawgAttribution(Modifier.padding(top = 8.dp))
        Text(
            "Playpick is a non-commercial portfolio project and isn't affiliated with RAWG or any platform holder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(FloatingBarClearance))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset your taste profile?") },
            text = { Text("Your likes and onboarding picks will be forgotten and you'll pick again. Your library and checklists are kept.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; viewModel.resetTaste() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title.uppercase(), style = HudLabel, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}
