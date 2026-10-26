package com.myth.movieapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.BuildConfig
import com.myth.movieapp.domain.model.ThemeMode
import com.myth.movieapp.ui.common.Card
import com.myth.movieapp.ui.common.Wordmark
import com.myth.movieapp.ui.theme.EyebrowStyle
import java.util.Locale

private val REGIONS = listOf(
    "US", "GB", "CA", "AU", "IE", "IN", "KE", "NG", "ZA", "DE", "FR", "ES", "IT", "NL", "BR", "MX", "JP", "KR",
)

private fun regionName(code: String) = Locale("", code).getDisplayCountry(Locale.getDefault())

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        Group("Preferences") {
            SettingRow(
                icon = Icons.Outlined.Public,
                title = "Streaming region",
                value = prefs.region?.let(::regionName) ?: "Automatic (${regionName(Locale.getDefault().country)})",
                onClick = { dialog = SettingsDialog.Region },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow(
                icon = Icons.Outlined.Palette,
                title = "Appearance",
                value = prefs.themeMode.label(),
                onClick = { dialog = SettingsDialog.Theme },
            )
        }
        Spacer(Modifier.height(20.dp))
        Group("Recommendations") {
            SettingRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Reset taste profile",
                value = "Forget ratings and genre picks, then start fresh",
                onClick = { dialog = SettingsDialog.Reset },
                destructive = true,
            )
        }
        Spacer(Modifier.height(32.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(markSize = 28.dp)
            Spacer(Modifier.height(6.dp))
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            // Attribution required by the TMDB API terms of use.
            Text(
                "This product uses the TMDB API but is not endorsed or certified by TMDB. " +
                    "Film data and images © TMDB. Streaming availability by JustWatch. " +
                    "Typefaces: Fraunces and Manrope, SIL Open Font License 1.1.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.themoviedb.org/".toUri())) }) {
                Text("themoviedb.org")
                Spacer(Modifier.size(4.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }

    when (dialog) {
        SettingsDialog.Region -> ChoiceDialog(
            title = "Streaming region",
            options = listOf<String?>(null) + REGIONS,
            selected = prefs.region,
            label = { it?.let(::regionName) ?: "Automatic" },
            onSelect = { viewModel.setRegion(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Theme -> ChoiceDialog(
            title = "Appearance",
            options = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM),
            selected = prefs.themeMode,
            label = { it.label() },
            onSelect = { viewModel.setTheme(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Reset -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Reset taste profile?") },
            text = { Text("Your ratings and genre picks will be deleted. Your watchlist is kept.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetTasteProfile(); dialog = null }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
        null -> Unit
    }
}

private enum class SettingsDialog { Region, Theme, Reset }

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "Match system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark (recommended)"
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    Card(Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
