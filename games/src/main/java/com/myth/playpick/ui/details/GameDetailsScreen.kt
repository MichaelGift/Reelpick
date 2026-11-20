package com.myth.playpick.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.CoverImage
import com.myth.core.design.GlassIconButton
import com.myth.core.design.MessageState
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.ui.common.AchievementIcon
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.GameRow
import com.myth.playpick.ui.common.GameRowItem
import com.myth.playpick.ui.common.LinearBar
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.SegmentedBar
import com.myth.playpick.ui.common.color
import com.myth.playpick.ui.common.percentLabel
import com.myth.playpick.ui.theme.Hud
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.ui.theme.HudMeta
import com.myth.playpick.ui.theme.HudStat
import java.util.Locale

@Composable
fun GameDetailsScreen(
    onBack: () -> Unit,
    onOpen: OpenGame,
    onChecklist: (gameId: Int, name: String) -> Unit,
    viewModel: GameDetailsViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val entry by viewModel.libraryEntry.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val loaded = content as? DetailsContent.Loaded
    val name = loaded?.details?.game?.name ?: viewModel.preview.name
    // Derived, so the screen recomposes when the threshold is crossed, not on every scrolled pixel.
    val pastHeader by remember { derivedStateOf { scroll.value > 520 } }
    val scrim by animateFloatAsState(if (pastHeader) 1f else 0f, label = "scrim")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // The art draws from the route at once, so the shared cover morph has a target.
            CoverImage(
                RawgImages.hero(loaded?.details?.game?.coverUrl ?: viewModel.preview.coverUrl),
                name,
                aspectRatio = COVER_RATIO,
                sharedKey = viewModel.sharedKey,
                shape = RoundedCornerShape(0.dp),
                placeholder = Icons.Rounded.SportsEsports,
            )
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                loaded?.details?.let { d ->
                    val studio = d.developers.firstOrNull()?.name
                    val year = d.game.releaseYear ?: "TBA".takeIf { d.game.tba }
                    listOfNotNull(studio, year).takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · ").uppercase(), style = HudLabel, color = Hud.Label)
                    }
                }
                Text(name, style = MaterialTheme.typography.displaySmall)
            }
            when (val c = content) {
                DetailsContent.Loading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(if (it == 0) 60.dp else 18.dp), RoundedCornerShape(12.dp)) }
                }
                is DetailsContent.Error -> MessageState(Icons.Outlined.CloudOff, "Couldn't load this game", c.message, Modifier.height(420.dp), "Try again", viewModel::load)
                is DetailsContent.Loaded -> Body(c, rating, entry, onOpen, viewModel::rate, viewModel::setStatus) { onChecklist(viewModel.gameId, name) }
            }
        }
        // Back and the title stay readable once the art scrolls away.
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = scrim)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp).alpha(scrim),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Body(
    c: DetailsContent.Loaded,
    rating: FeedbackType?,
    entry: LibraryEntry?,
    onOpen: OpenGame,
    onRate: (FeedbackType) -> Unit,
    onStatus: (LibraryStatus?) -> Unit,
    onChecklist: () -> Unit,
) {
    val details = c.details
    val game = details.game
    val uri = LocalUriHandler.current
    Column(Modifier.navigationBarsPadding().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // RAWG has "RPG" as both a genre and a tag; show each label once.
                (game.tags.take(4) + game.genres).distinctBy { it.name.lowercase() }.take(5).forEach { Chip(it.name) }
                if (game.platforms.isNotEmpty()) Chip(game.platforms.joinToString(" · ") { shortPlatform(it.name) }, mono = true)
            }

            StatsStrip(
                listOf(
                    "RATING" to (game.rating.takeIf { game.ratingsCount > 0 }?.let { String.format(Locale.US, "★%.1f", it) } ?: "—"),
                    "META" to (game.metacritic?.toString() ?: "—"),
                    "TO BEAT" to (game.playtimeHours.takeIf { it > 0 }?.let { "${it}H" } ?: "—"),
                    "TROPHIES" to (details.achievementCount.takeIf { it > 0 }?.toString() ?: "—"),
                ),
                metacritic = game.metacritic,
            )

            c.fit?.let { fit ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("TASTE MATCH", style = HudLabel, color = Hud.Label, modifier = Modifier.weight(1f))
                        Text("${fit.match}%", style = HudStat.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.primary)
                    }
                    SegmentedBar(fit.match / 100f)
                    if (fit.reasons.isNotEmpty()) {
                        Text(
                            buildAnnotatedString {
                                append("You tend to love ")
                                fit.reasons.forEachIndexed { i, r ->
                                    if (i > 0) append(if (i == fit.reasons.lastIndex) " and " else ", ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append(r) }
                                }
                                append(".")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("STATUS", style = HudLabel, color = Hud.Label)
                StatusSwitch(entry?.status, onStatus)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Toggle(if (rating == FeedbackType.LIKE) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp, "Loved it", rating == FeedbackType.LIKE, Modifier.weight(1f)) { onRate(FeedbackType.LIKE) }
                Toggle(Icons.Outlined.Block, "Not for me", rating == FeedbackType.NOT_INTERESTED, Modifier.weight(1f)) { onRate(FeedbackType.NOT_INTERESTED) }
            }
        }

        if (details.achievementCount > 0) CompletionCard(c, entry, onChecklist)

        if (details.description.isNotBlank()) Description(details.description)

        if (details.screenshots.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Screenshots", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(details.screenshots) { shot ->
                        CoverImage(RawgImages.cover(shot), null, Modifier.width(260.dp), aspectRatio = COVER_RATIO, shape = RoundedCornerShape(12.dp), placeholder = null)
                    }
                }
            }
        }

        if (details.stores.isNotEmpty() || details.website != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("WHERE TO GET IT", style = HudLabel, color = Hud.Label, modifier = Modifier.padding(horizontal = 16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(details.stores) { link -> LinkChip(link.store.name) { uri.openUri(link.url) } }
                    details.website?.let { site -> item { LinkChip("Official site") { uri.openUri(site) } } }
                }
            }
        }

        val publishers = details.publishers.filter { p -> details.developers.none { it.id == p.id } }
        if (publishers.isNotEmpty()) {
            Text(
                "Published by " + publishers.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (c.series.isNotEmpty()) GameRow("In the series", c.series.map { GameRowItem(it) }, "series", onOpen)
        if (c.additions.isNotEmpty()) GameRow("DLC & editions", c.additions.map { GameRowItem(it) }, "dlc", onOpen)
        if (c.similar.isNotEmpty()) GameRow("More like this", c.similar.map { (g, m) -> GameRowItem(g, match = m) }, "similar", onOpen, caption = "Shares its tags")
        RawgAttribution(Modifier.padding(bottom = 8.dp))
    }
}

private fun shortPlatform(name: String) = when (name) {
    "PlayStation" -> "PS"
    "Apple Macintosh" -> "MAC"
    "Nintendo" -> "SWITCH"
    else -> name.uppercase()
}

/** Four stats in one hairline-divided strip, like a game's own stat screen. */
@Composable
private fun StatsStrip(stats: List<Pair<String, String>>, metacritic: Int?) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.outlineVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        stats.forEach { (label, value) ->
            Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = HudLabel.copy(fontSize = 10.sp), color = Hud.Label)
                val tint = if (label == "META" && metacritic != null) metacriticColor(metacritic) else MaterialTheme.colorScheme.onSurface
                Text(value, style = HudStat, color = tint, maxLines = 1)
            }
        }
    }
}

private fun metacriticColor(score: Int) = when {
    score >= 75 -> Color(0xFF4ADE80)
    score >= 50 -> Color(0xFFF5B83D)
    else -> Color(0xFFFF6874)
}

/** One control for library status; tapping the active segment takes the game out of the library. */
@Composable
private fun StatusSwitch(current: LibraryStatus?, onStatus: (LibraryStatus?) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LibraryStatus.entries.forEach { status ->
            val selected = current == status
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onStatus(if (selected) null else status) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (status == LibraryStatus.COMPLETED) "Done" else status.label,
                    style = if (status == LibraryStatus.MASTERED) HudStat.copy(fontSize = 12.sp) else MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(c: DetailsContent.Loaded, entry: LibraryEntry?, onChecklist: () -> Unit) {
    val total = c.details.achievementCount
    val progress = entry?.progress?.takeIf { it.total > 0 }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(Hud.CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outlineVariant, Hud.CardShape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Completion", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("${progress?.unlocked ?: 0}") }
                withStyle(SpanStyle(color = Hud.Label)) { append(" / ${progress?.total ?: total}") }
            }, style = HudStat.copy(fontSize = 14.sp))
        }
        LinearBar(progress?.fraction ?: 0f, height = 8.dp)
        c.rarest.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AchievementIcon(a, unlocked = true, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                a.rarity?.let { r -> Text(a.percentLabel().orEmpty(), style = HudStat.copy(fontSize = 11.sp), color = r.color) }
            }
        }
        Surface(
            onClick = onChecklist,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = Hud.ControlShape,
            color = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (progress != null) "Open checklist" else "Track completion", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun Description(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("ABOUT", style = HudLabel, color = Hud.Label)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Show less" else "Read more", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Chip(label: String, mono: Boolean = false) {
    Surface(
        shape = Hud.TagShape,
        color = if (mono) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (mono) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        border = if (mono) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Text(
            label,
            style = if (mono) HudMeta.copy(fontSize = 11.sp) else MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun LinkChip(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = Hud.ControlShape, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Toggle(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = Hud.ControlShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
