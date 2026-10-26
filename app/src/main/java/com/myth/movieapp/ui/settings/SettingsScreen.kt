package com.myth.movieapp.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.BuildConfig
import com.myth.movieapp.domain.model.ThemeMode
import java.util.Locale

private val REGIONS = listOf(
    "US", "GB", "CA", "AU", "IE", "IN", "KE", "NG", "ZA", "DE", "FR", "ES", "IT", "NL", "BR", "MX", "JP", "KR",
)

private fun regionName(code: String) = Locale("", code).getDisplayCountry(Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text("Streaming region") },
                supportingContent = {
                    Text(prefs.region?.let(::regionName) ?: "Device default (${regionName(Locale.getDefault().country)})")
                },
                leadingContent = { Icon(Icons.Outlined.Public, contentDescription = null) },
                modifier = Modifier.clickable { dialog = SettingsDialog.Region },
            )
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(prefs.themeMode.label()) },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                modifier = Modifier.clickable { dialog = SettingsDialog.Theme },
            )
            ListItem(
                headlineContent = { Text("Reset taste profile") },
                supportingContent = { Text("Forget likes, dislikes and genre picks, then start onboarding again") },
                leadingContent = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                modifier = Modifier.clickable { dialog = SettingsDialog.Reset },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
            )
            // Attribution required by the TMDB API terms of use.
            Text(
                "This product uses the TMDB API but is not endorsed or certified by TMDB. " +
                    "Movie data and images © TMDB. Streaming availability provided by JustWatch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/"))) },
                modifier = Modifier.padding(horizontal = 4.dp),
            ) { Text("themoviedb.org") }
        }
    }

    when (dialog) {
        SettingsDialog.Region -> ChoiceDialog(
            title = "Streaming region",
            options = listOf<String?>(null) + REGIONS,
            selected = prefs.region,
            label = { it?.let(::regionName) ?: "Device default" },
            onSelect = { viewModel.setRegion(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Theme -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries,
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
                TextButton(onClick = { viewModel.resetTasteProfile(); dialog = null }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
        null -> Unit
    }
}

private enum class SettingsDialog { Region, Theme, Reset }

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
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
                        Modifier.clickable { onSelect(option) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
