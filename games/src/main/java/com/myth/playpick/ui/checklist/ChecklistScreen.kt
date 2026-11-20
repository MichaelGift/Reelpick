package com.myth.playpick.ui.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.Brand
import com.myth.core.design.MessageState
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.ChecklistItem
import com.myth.playpick.domain.model.Progress
import com.myth.playpick.domain.model.Rarity
import com.myth.playpick.ui.common.AchievementIcon
import com.myth.playpick.ui.common.HudPill
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.SegmentedBar
import com.myth.playpick.ui.common.color
import com.myth.playpick.ui.common.label
import com.myth.playpick.ui.common.percentLabel
import com.myth.playpick.ui.theme.Hud
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.ui.theme.HudStat

@Composable
fun ChecklistScreen(onBack: () -> Unit, viewModel: ChecklistViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val checklist = state.checklist
    val haptics = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBack, shape = Hud.ControlShape, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("CHECKLIST", style = HudLabel.copy(fontSize = 10.sp), color = Hud.Label)
                Text(state.entry?.game?.name ?: viewModel.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = viewModel::refresh, enabled = !state.isSyncing) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh the list") }
        }
        when {
            checklist == null || (checklist.syncedAtMillis == null && state.isSyncing) -> Loading()
            checklist.items.isEmpty() && state.error != null -> MessageState(Icons.Outlined.CloudOff, "Couldn't load achievements", state.error!!, actionLabel = "Try again", onAction = viewModel::refresh)
            checklist.items.isEmpty() -> MessageState(
                Icons.Outlined.EmojiEvents,
                "No achievements listed",
                "RAWG has no achievement list for this game. You can still track it in your library.",
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.navigationBarsPadding()) {
                item(key = "header") { ProgressCard(checklist.progress, checklist.items) }
                item(key = "controls") { Controls(state.controls, viewModel::setFilter, viewModel::setSort, viewModel::setQuery) }
                if (state.visible.isEmpty()) {
                    item(key = "none") {
                        Text("Nothing here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                }
                items(state.visible, key = { it.achievement.id }) { item ->
                    AchievementRow(item) {
                        haptics.performHapticFeedback(if (item.unlocked) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                        viewModel.toggle(item)
                    }
                }
                item(key = "attribution") { RawgAttribution() }
            }
        }
    }

    if (state.offerMastered) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMastered,
            icon = { Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Hud.Gold) },
            title = { Text("Every achievement unlocked") },
            text = { Text("Mark ${state.entry?.game?.name ?: "this game"} as 100%? It's the strongest signal for your recommendations.") },
            confirmButton = { TextButton(onClick = viewModel::markMastered) { Text("Mark 100%") } },
            dismissButton = { TextButton(onClick = viewModel::dismissMastered) { Text("Not yet") } },
        )
    }
}

/** The big count, the XP bar and what's left at the top rarities. */
@Composable
private fun ProgressCard(progress: Progress, items: List<ChecklistItem>) {
    val complete = progress.isComplete
    val accent = if (complete) Hud.Gold else MaterialTheme.colorScheme.primary
    val left = { r: Rarity -> items.count { !it.unlocked && it.achievement.rarity == r } }
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(Hud.CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outlineVariant, Hud.CardShape).padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${progress.unlocked} of ${progress.total} achievements unlocked" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = accent, fontSize = 44.sp)) { append("${progress.unlocked}") }
                    withStyle(SpanStyle(color = Hud.Label, fontSize = 20.sp)) { append(" / ${progress.total}") }
                },
                style = HudStat.copy(lineHeight = 44.sp),
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("${(progress.fraction * 100).toInt()}%", style = HudStat.copy(fontSize = 20.sp))
                Text(if (complete) "LEGENDARY" else "COMPLETE", style = HudLabel.copy(fontSize = 10.sp), color = Hud.Label)
            }
        }
        SegmentedBar(progress.fraction, segments = 24, height = 10.dp, color = accent)
        val ultra = left(Rarity.ULTRA_RARE)
        val rare = left(Rarity.RARE)
        if (ultra + rare > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RarityLeft(Rarity.ULTRA_RARE, ultra)
                RarityLeft(Rarity.RARE, rare)
            }
        }
    }
}

@Composable
private fun RarityLeft(rarity: Rarity, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(rarity.color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("$count ${rarity.label.lowercase()} left", style = HudLabel.copy(letterSpacing = 0.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A smaller progress ring, still used on library rows. */
@Composable
fun ProgressRing(progress: Progress, size: Dp, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    val fraction by animateFloatAsState(progress.fraction, tween(600), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = if (progress.isComplete) Hud.Gold else Brand.tokens.gradientColors.first()
    Box(modifier.size(size).semantics { contentDescription = "${(progress.fraction * 100).toInt()} percent complete" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = this.size.minDimension * 0.1f
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(Brush.linearGradient(listOf(color, color)), -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (showLabel) Text("${(progress.fraction * 100).toInt()}%", style = HudStat.copy(fontSize = if (size >= 80.dp) 20.sp else 11.sp))
    }
}

@Composable
private fun Controls(
    controls: ChecklistControls,
    onFilter: (ChecklistFilter) -> Unit,
    onSort: (ChecklistSort) -> Unit,
    onQuery: (String) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ChecklistFilter.entries) { HudPill(it.label, controls.filter == it, { onFilter(it) }) }
            item { Spacer(Modifier.width(10.dp)) }
            items(ChecklistSort.entries) { HudPill(it.label, controls.sort == it, { onSort(it) }, mono = false) }
        }
        TextField(
            value = controls.query,
            onValueChange = onQuery,
            placeholder = { Text("Find an achievement") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = { if (controls.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") } },
            singleLine = true,
            shape = Hud.ControlShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun AchievementRow(item: ChecklistItem, onToggle: () -> Unit) {
    val a = item.achievement
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (item.unlocked) "Unlocked" else "Locked"
            }
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AchievementIcon(a, item.unlocked, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).alpha(if (item.unlocked) 0.6f else 1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(a.name, style = MaterialTheme.typography.titleSmall, textDecoration = if (item.unlocked) TextDecoration.LineThrough else null)
            if (a.description.isNotBlank()) {
                Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            a.rarity?.let { r ->
                Text(
                    listOfNotNull(r.label.uppercase(), a.percentLabel()).joinToString(" · "),
                    style = HudStat.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                    color = r.color,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        CheckBox(item.unlocked)
    }
}

/** A square HUD checkbox: an outline when locked, an accent tile with a tick when unlocked. */
@Composable
private fun CheckBox(checked: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.size(26.dp).clip(shape)
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(2.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Loading() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(130.dp), RoundedCornerShape(16.dp))
        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(12.dp)) }
    }
}
