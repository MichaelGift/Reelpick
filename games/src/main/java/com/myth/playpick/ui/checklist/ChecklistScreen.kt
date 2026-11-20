package com.myth.playpick.ui.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.Brand
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.MessageState
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.ChecklistItem
import com.myth.playpick.domain.model.Progress
import com.myth.playpick.ui.common.AchievementIcon
import com.myth.playpick.ui.common.RarityBadge
import com.myth.playpick.ui.common.RawgAttribution

@Composable
fun ChecklistScreen(onBack: () -> Unit, viewModel: ChecklistViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val checklist = state.checklist
    val haptics = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                state.entry?.game?.name ?: viewModel.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
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
                item(key = "header") { Header(checklist.progress, state.entry?.game?.playtimeHours ?: 0) }
                item(key = "controls") { Controls(state.controls, viewModel::setFilter, viewModel::setSort, viewModel::setQuery) }
                if (state.visible.isEmpty()) {
                    item(key = "none") {
                        Text("Nothing here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
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
            icon = { Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Brand.tokens.rating) },
            title = { Text("Every achievement unlocked") },
            text = { Text("Mark ${state.entry?.game?.name ?: "this game"} as 100%? It's the strongest signal for your recommendations.") },
            confirmButton = { TextButton(onClick = viewModel::markMastered) { Text("Mark 100%") } },
            dismissButton = { TextButton(onClick = viewModel::dismissMastered) { Text("Not yet") } },
        )
    }
}

@Composable
private fun Header(progress: Progress, playtimeHours: Int) {
    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        ProgressRing(progress, size = 96.dp)
        Spacer(Modifier.width(20.dp))
        Column {
            Text("COMPLETION", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("${progress.unlocked} / ${progress.total}", style = MaterialTheme.typography.headlineMedium)
            Text(
                when {
                    progress.isComplete -> "All done. Legendary."
                    playtimeHours > 0 -> "${progress.total - progress.unlocked} to go · about $playtimeHours h to beat"
                    else -> "${progress.total - progress.unlocked} to go"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An animated ring on the brand gradient, with the percentage in the middle. */
@Composable
fun ProgressRing(progress: Progress, size: Dp, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    val fraction by animateFloatAsState(progress.fraction, tween(600), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val colors = Brand.tokens.gradientColors
    Box(modifier.size(size).semantics { contentDescription = "${(progress.fraction * 100).toInt()} percent complete" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = this.size.minDimension * 0.1f
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(
                androidx.compose.ui.graphics.Brush.sweepGradient(colors + colors.first()),
                -90f, 360f * fraction, false, Offset(inset, inset), arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (showLabel) Text("${(progress.fraction * 100).toInt()}%", style = if (size >= 80.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Controls(
    controls: ChecklistControls,
    onFilter: (ChecklistFilter) -> Unit,
    onSort: (ChecklistSort) -> Unit,
    onQuery: (String) -> Unit,
) {
    Column(Modifier.padding(bottom = 8.dp)) {
        TextField(
            value = controls.query,
            onValueChange = onQuery,
            placeholder = { Text("Find an achievement") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = { if (controls.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") } },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ChecklistFilter.entries) { Pill(it.label, controls.filter == it) { onFilter(it) } }
            item { Spacer(Modifier.width(8.dp)) }
            items(ChecklistSort.entries) { Pill(it.label, controls.sort == it, subtle = true) { onSort(it) } }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, subtle: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = when {
            selected && !subtle -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when {
            selected && !subtle -> MaterialTheme.colorScheme.onPrimary
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun AchievementRow(item: ChecklistItem, onToggle: () -> Unit) {
    val a = item.achievement
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (item.unlocked) "Unlocked" else "Locked"
            }
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AchievementIcon(a, item.unlocked)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                a.name,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = if (item.unlocked) TextDecoration.LineThrough else null,
                color = if (item.unlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (a.description.isNotBlank()) {
                Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (a.rarity != null) {
                Spacer(Modifier.height(6.dp))
                RarityBadge(a)
            }
        }
        Spacer(Modifier.width(12.dp))
        CheckCircle(item.unlocked)
    }
}

@Composable
private fun CheckCircle(checked: Boolean) {
    val shape = CircleShape
    Box(
        Modifier.size(30.dp).clip(shape).then(
            if (checked) Modifier.background(Brand.tokens.gradient) else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Loading() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShimmerBox(Modifier.size(96.dp), CircleShape)
        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(14.dp)) }
    }
}
