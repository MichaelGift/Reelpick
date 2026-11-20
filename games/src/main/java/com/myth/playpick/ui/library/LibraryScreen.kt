package com.myth.playpick.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.CoverImage
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.MessageState
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.ui.checklist.ProgressRing
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.gameKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpen: OpenGame,
    onChecklist: (gameId: Int, name: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetFor by remember { mutableStateOf<LibraryEntry?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Library", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 20.dp, top = 20.dp))
        if (!state.isEmpty) {
            Text(
                plural(state.counts.values.sum(), "game") + " · " + plural(state.unlockedTotal, "achievement") + " unlocked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        if (state.isEmpty) {
            MessageState(
                Icons.Outlined.VideoLibrary,
                "Your library is empty",
                "Add games from any page, or long-press a card, to track what you play and every achievement you unlock.",
            )
            return@Column
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LibraryViewModel.ORDER) { status ->
                val count = state.counts[status] ?: 0
                val selected = state.filter == status
                Surface(
                    onClick = { viewModel.setFilter(status) },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text("${status.label}  $count", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), modifier = Modifier.navigationBarsPadding()) {
            state.sections.forEach { section ->
                item(key = "h-${section.status}") {
                    Text(
                        section.status.label.uppercase(),
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(section.entries, key = { it.game.id }) { entry ->
                    EntryRow(entry, onOpen, { onChecklist(entry.game.id, entry.game.name) }, onLongClick = { sheetFor = entry })
                }
            }
        }
    }

    sheetFor?.let { entry ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(entry.game.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                LibraryViewModel.ORDER.filter { it != entry.status }.forEach { status ->
                    ListItem(
                        headlineContent = { Text("Move to ${status.label}") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickableRow { viewModel.setStatus(entry, status); sheetFor = null },
                    )
                }
                ListItem(
                    headlineContent = { Text("Remove from library", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickableRow { viewModel.remove(entry); sheetFor = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.clickableRow(onClick: () -> Unit) = combinedClickable(onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(entry: LibraryEntry, onOpen: OpenGame, onChecklist: () -> Unit, onLongClick: () -> Unit) {
    val key = gameKey("library", entry.game.id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = { onOpen(entry.game, key) }, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(RawgImages.cover(entry.game.coverUrl), entry.game.name, Modifier.width(120.dp), aspectRatio = COVER_RATIO, sharedKey = key, placeholder = Icons.Rounded.SportsEsports)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.game.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (entry.progress.total > 0) "${entry.progress.unlocked} of ${entry.progress.total} achievements" else "No checklist yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (entry.progress.total > 0) {
            IconButton(onClick = onChecklist, modifier = Modifier.width(52.dp).height(52.dp)) { ProgressRing(entry.progress, size = 44.dp) }
        } else {
            IconButton(onClick = onChecklist) { Icon(Icons.Rounded.Checklist, contentDescription = "Open checklist", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

private fun plural(n: Int, noun: String) = "$n $noun" + if (n == 1) "" else "s"
