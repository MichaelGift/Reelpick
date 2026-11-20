package com.myth.playpick.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myth.core.design.Brand
import com.myth.core.design.CoverImage
import com.myth.core.design.Fraunces
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.core.design.MatchBadge
import com.myth.core.design.PosterShape
import com.myth.core.design.SectionHeader
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.LibraryStatus
import java.util.Locale

/** Open a game's details; [sharedKey] lets the tapped cover morph into the details header. */
typealias OpenGame = (game: Game, sharedKey: String?) -> Unit

/** Unique per on-screen occurrence: the same game can appear in several rows at once. */
fun gameKey(section: String, gameId: Int) = "cover-$section-$gameId"

/** RAWG artwork is landscape (screenshots and key art), so cards are 16:9 rather than posters. */
const val COVER_RATIO = 16f / 9f

/** "2017 · 27 h · ★ 4.4" */
fun Game.metaLine(): String = listOfNotNull(
    releaseYear ?: if (tba) "TBA" else null,
    playtimeHours.takeIf { it > 0 }?.let { "$it h" },
    rating.takeIf { ratingsCount > 0 && it > 0 }?.let { "★ " + String.format(Locale.US, "%.1f", it) },
).joinToString("  ·  ")

data class GameRowItem(val game: Game, val caption: String? = null, val match: Int? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Null fills the available width, as in a grid. */
    width: Dp? = 232.dp,
    caption: String? = null,
    match: Int? = null,
    sharedKey: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clip(PosterShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.shadow(10.dp, PosterShape, ambientColor = Color.Black, spotColor = Color.Black)) {
            CoverImage(RawgImages.cover(game.coverUrl), game.name, aspectRatio = COVER_RATIO, sharedKey = sharedKey, placeholder = Icons.Rounded.SportsEsports)
            if (match != null && match >= MIN_DISPLAY_MATCH) MatchBadge(match, Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(game.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            caption ?: game.metaLine(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GameRow(
    title: String,
    items: List<GameRowItem>,
    section: String,
    onOpen: OpenGame,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onLongClick: ((Game) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(title, eyebrow = eyebrow, subtitle = subtitle)
        Spacer(Modifier.height(14.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(items, key = { _, item -> item.game.id }) { _, item ->
                val key = gameKey(section, item.game.id)
                GameCard(
                    game = item.game,
                    caption = item.caption,
                    match = item.match,
                    sharedKey = key,
                    onClick = { onOpen(item.game, key) },
                    onLongClick = onLongClick?.let { { it(item.game) } },
                )
            }
        }
    }
}

/** "Top 10" row: outlined rank numerals beside each card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankedGameRow(
    title: String,
    items: List<GameRowItem>,
    section: String,
    onOpen: OpenGame,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onLongClick: ((Game) -> Unit)? = null,
) {
    val numeral = TextStyle(fontFamily = Fraunces, fontSize = 104.sp, lineHeight = 104.sp, drawStyle = Stroke(width = 5f))
    Column(modifier) {
        SectionHeader(title, eyebrow = eyebrow)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(items, key = { _, item -> item.game.id }) { index, item ->
                val key = gameKey(section, item.game.id)
                Row(
                    Modifier.combinedClickable(onClick = { onOpen(item.game, key) }, onLongClick = onLongClick?.let { { it(item.game) } }),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "${index + 1}",
                        style = numeral,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.offset(x = 8.dp, y = 14.dp),
                    )
                    Box(Modifier.width(220.dp).shadow(12.dp, PosterShape)) {
                        CoverImage(RawgImages.cover(item.game.coverUrl), item.game.name, aspectRatio = COVER_RATIO, sharedKey = key, placeholder = Icons.Rounded.SportsEsports)
                        item.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let { MatchBadge(it, Modifier.align(Alignment.BottomStart).padding(8.dp)) }
                    }
                }
            }
        }
    }
}

/** The app mark: a controller glyph on the brand gradient, beside the name in Fraunces. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, markSize: Dp = 30.dp, color: Color = MaterialTheme.colorScheme.onBackground) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(markSize).clip(RoundedCornerShape(markSize * 0.28f)).background(Brand.tokens.gradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = Color.White, modifier = Modifier.size(markSize * 0.66f))
        }
        Spacer(Modifier.width(10.dp))
        Text("Playpick", style = MaterialTheme.typography.headlineSmall.copy(fontSize = (markSize.value * 0.95f).sp), color = color)
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
            Text(game.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SheetAction(Icons.Outlined.ThumbUp, "Loved it") { onFeedback(FeedbackType.LIKE) }
            SheetAction(Icons.Outlined.BookmarkAdd, "Add to backlog") { onStatus(LibraryStatus.BACKLOG) }
            SheetAction(Icons.Rounded.SportsEsports, "I'm playing this") { onStatus(LibraryStatus.PLAYING) }
            SheetAction(Icons.Outlined.ThumbDown, "Not for me") { onFeedback(FeedbackType.DISLIKE) }
            SheetAction(Icons.Outlined.Block, "Don't show this again") { onFeedback(FeedbackType.NOT_INTERESTED) }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clip(CircleShape).combinedClickableCompat(onClick),
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = combinedClickable(onClick = onClick)
