package com.myth.playpick.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.myth.core.design.CoverImage
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.ui.theme.Hud
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.ui.theme.HudMeta
import com.myth.playpick.ui.theme.HudStat
import java.util.Locale

/** Open a game's details; [sharedKey] lets the tapped cover morph into the details header. */
typealias OpenGame = (game: Game, sharedKey: String?) -> Unit

/** Unique per on-screen occurrence: the same game can appear in several rows at once. */
fun gameKey(section: String, gameId: Int) = "cover-$section-$gameId"

/** RAWG artwork is landscape (screenshots and key art), so cards are 16:9 rather than posters. */
const val COVER_RATIO = 16f / 9f

private val CoverShape = RoundedCornerShape(12.dp)

/** "2017 · 27 H · ★ 4.4", in the HUD's mono caps. */
fun Game.metaLine(): String = listOfNotNull(
    releaseYear ?: if (tba) "TBA" else null,
    playtimeHours.takeIf { it > 0 }?.let { "$it H" },
    rating.takeIf { ratingsCount > 0 && it > 0 }?.let { "★ " + String.format(Locale.US, "%.1f", it) },
).joinToString(" · ")

data class GameRowItem(val game: Game, val caption: String? = null, val match: Int? = null)

/** The accent-filled tag: "92% MATCH" on a hero, "92%" on a card. */
@Composable
fun MatchTag(match: Int, modifier: Modifier = Modifier, long: Boolean = false) {
    Box(
        modifier.height(if (long) 26.dp else 22.dp).clip(Hud.TagShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = if (long) 10.dp else 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (long) "$match% MATCH" else "$match%",
            style = HudStat.copy(fontSize = if (long) 12.sp else 11.sp),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** A dark tag that sits on artwork: counters, ranks, labels. */
@Composable
fun HudTag(text: String, modifier: Modifier = Modifier, color: Color = Hud.Text) {
    Box(modifier.height(26.dp).clip(Hud.TagShape).background(Hud.Ink.copy(alpha = 0.74f)).padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, style = HudStat.copy(fontSize = 11.sp), color = color)
    }
}

/** Segmented XP-style progress: [segments] blocks, the first `fraction` of them lit. */
@Composable
fun SegmentedBar(fraction: Float, modifier: Modifier = Modifier, segments: Int = 20, height: Dp = 8.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val lit = (fraction.coerceIn(0f, 1f) * segments).let { if (fraction > 0f && it < 1f) 1 else it.toInt() }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            Box(
                Modifier.weight(1f).height(height).clip(RoundedCornerShape(2.dp))
                    .background(if (i < lit) color else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** A plain bar for compact rows; gold once complete. */
@Composable
fun LinearBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val complete = fraction >= 1f
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(MaterialTheme.colorScheme.outlineVariant)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height)
                .background(if (complete) Hud.Gold else MaterialTheme.colorScheme.primary),
        )
    }
}

/** "Top 10 for you" on the left, a mono caption ("RANKED BY TASTE") on the right. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, caption: String? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (caption != null) {
            Spacer(Modifier.width(12.dp))
            Text(caption.uppercase(), style = HudLabel, color = Hud.Label, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Null fills the available width, as in a grid. */
    width: Dp? = 200.dp,
    caption: String? = null,
    match: Int? = null,
    rank: Int? = null,
    sharedKey: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clip(CoverShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CoverShape)) {
            CoverImage(RawgImages.cover(game.coverUrl), game.name, aspectRatio = COVER_RATIO, sharedKey = sharedKey, shape = CoverShape, placeholder = Icons.Rounded.SportsEsports)
            if (rank != null) {
                Text(
                    rank.toString().padStart(2, '0'),
                    style = HudStat.copy(fontSize = 30.sp, lineHeight = 30.sp),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 6.dp),
                )
            }
            if (match != null && match >= MIN_DISPLAY_MATCH) {
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).height(22.dp).clip(Hud.TagShape).background(Hud.Ink.copy(alpha = 0.82f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                    Text("$match%", style = HudStat.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(game.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Text(caption ?: game.metaLine(), style = HudMeta.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GameRow(
    title: String,
    items: List<GameRowItem>,
    section: String,
    onOpen: OpenGame,
    modifier: Modifier = Modifier,
    caption: String? = null,
    cardWidth: Dp = 200.dp,
    ranked: Boolean = false,
    onLongClick: ((Game) -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title, caption = caption)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(items, key = { _, item -> item.game.id }) { index, item ->
                val key = gameKey(section, item.game.id)
                GameCard(
                    game = item.game,
                    caption = item.caption,
                    match = item.match,
                    rank = if (ranked) index + 1 else null,
                    width = cardWidth,
                    sharedKey = key,
                    onClick = { onOpen(item.game, key) },
                    onLongClick = onLongClick?.let { { it(item.game) } },
                )
            }
        }
    }
}

/** The app mark: a controller glyph on a lime tile, beside the name in Space Grotesk caps. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, markSize: Dp = 32.dp, color: Color = MaterialTheme.colorScheme.onBackground) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(markSize).clip(RoundedCornerShape(markSize * 0.25f)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(markSize * 0.62f))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "PLAYPICK",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = (markSize.value * 0.62f).sp, letterSpacing = 0.06.em),
            color = color,
        )
    }
}

/** A row of HUD pills, one selected: platform switches, filters, sorts. */
@Composable
fun HudPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, mono: Boolean = true) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                if (mono) label.uppercase() else label,
                style = if (mono) HudStat.copy(fontSize = 12.sp, letterSpacing = 0.04.em) else MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Long-press actions on any game card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameActionsSheet(
    game: Game,
    onDismiss: () -> Unit,
    onFeedback: (FeedbackType) -> Unit,
    onStatus: (LibraryStatus) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(game.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SheetAction(Icons.Outlined.ThumbUp, "Loved it") { onFeedback(FeedbackType.LIKE) }
            SheetAction(Icons.Outlined.BookmarkAdd, "Add to backlog") { onStatus(LibraryStatus.BACKLOG) }
            SheetAction(Icons.Rounded.SportsEsports, "I'm playing this") { onStatus(LibraryStatus.PLAYING) }
            SheetAction(Icons.Outlined.ThumbDown, "Not for me") { onFeedback(FeedbackType.DISLIKE) }
            SheetAction(Icons.Outlined.Block, "Don't show this again") { onFeedback(FeedbackType.NOT_INTERESTED) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clip(Hud.ControlShape).combinedClickable(onClick = onClick),
    )
}
