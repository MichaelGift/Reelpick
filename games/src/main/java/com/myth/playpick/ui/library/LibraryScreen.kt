package com.myth.playpick.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.CoverImage
import com.myth.core.design.MessageState
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.HudPill
import com.myth.playpick.ui.common.LinearBar
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.gameKey
import com.myth.playpick.ui.navigation.FloatingBarClearance
import com.myth.playpick.ui.theme.Hud
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.ui.theme.HudMeta
import com.myth.playpick.ui.theme.HudStat

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
        Text("Library", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 14.dp))
        if (state.isEmpty) {
            MessageState(
                Icons.Outlined.VideoLibrary,
                "Your library is empty",
                "Add games from any page, or long-press a card, to track what you play and every achievement you unlock.",
            )
            return@Column
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("GAMES", state.counts.values.sum().toString(), Modifier.weight(1f))
            StatTile("100%", (state.counts[LibraryStatus.MASTERED] ?: 0).toString(), Modifier.weight(1f), accent = true)
            StatTile("TROPHIES", state.unlockedTotal.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(LibraryViewModel.ORDER) { status ->
                val label = if (status == LibraryStatus.COMPLETED) "Done" else status.label
                HudPill("$label  ${state.counts[status] ?: 0}", state.filter == status, { viewModel.setFilter(status) }, mono = false)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = FloatingBarClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            state.sections.forEach { section ->
                item(key = "h-${section.status}") {
                    Text(
                        (if (section.status == LibraryStatus.COMPLETED) "Done" else section.status.label).uppercase(),
                        style = HudLabel,
                        color = Hud.Label,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
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
                Text(entry.game.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
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

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = HudLabel.copy(fontSize = 10.sp), color = Hud.Label)
        Text(value, style = HudStat.copy(fontSize = 22.sp), color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.clickableRow(onClick: () -> Unit) = combinedClickable(onClick = onClick)

/** Tapping opens the game; tapping its progress opens the checklist; long-press moves or removes it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(entry: LibraryEntry, onOpen: OpenGame, onChecklist: () -> Unit, onLongClick: () -> Unit) {
    val key = gameKey("library", entry.game.id)
    val p = entry.progress
    val complete = p.isComplete
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .combinedClickable(onClick = { onOpen(entry.game, key) }, onLongClick = onLongClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(RawgImages.cover(entry.game.coverUrl), entry.game.name, Modifier.width(96.dp), aspectRatio = COVER_RATIO, sharedKey = key, shape = RoundedCornerShape(9.dp), placeholder = Icons.Rounded.SportsEsports)
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = onChecklist).padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.game.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (p.total > 0) "${(p.fraction * 100).toInt()}%" else "—",
                    style = HudStat.copy(fontSize = 12.sp),
                    color = if (complete) Hud.Gold else MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearBar(if (p.total > 0) p.fraction else 0f)
            Text(
                when {
                    p.total == 0 -> "OPEN CHECKLIST"
                    complete -> "ALL ${p.total} UNLOCKED"
                    else -> "${p.unlocked} / ${p.total} ACHIEVEMENTS"
                },
                style = HudMeta.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
