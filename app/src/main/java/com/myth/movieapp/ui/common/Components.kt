package com.myth.movieapp.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.ui.theme.Brand
import com.myth.movieapp.ui.theme.EyebrowStyle
import com.myth.movieapp.ui.theme.Fraunces
import java.util.Locale

/** One poster in a row, with optional caption and taste match. */
data class RowItem(val movie: Movie, val caption: String? = null, val match: Int? = null)

val PosterShape = RoundedCornerShape(14.dp)

/** Matches below this are real but read as "not for you", so they are not advertised on posters. */
const val MIN_DISPLAY_MATCH = 60

@Composable
fun PosterImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    shape: Shape = PosterShape,
    showPlaceholderIcon: Boolean = true,
) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .sharedPoster(sharedKey)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholderIcon) {
            Icon(Icons.Outlined.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        AsyncImage(
            model = TmdbImages.poster(path),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Hairline inner edge so dark posters don't dissolve into the dark background.
        Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.06f), shape))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp,
    caption: String? = null,
    match: Int? = null,
    sharedKey: String? = null,
    showTitle: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(PosterShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.shadow(10.dp, PosterShape, ambientColor = Color.Black, spotColor = Color.Black)) {
            PosterImage(movie.posterPath, contentDescription = movie.title, sharedKey = sharedKey)
            if (match != null && match >= MIN_DISPLAY_MATCH) {
                MatchBadge(match, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        if (showTitle) {
            Spacer(Modifier.height(8.dp))
            Text(
                movie.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption ?: listOfNotNull(movie.releaseYear, movie.ratingLabel()).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun Movie.ratingLabel(): String? = if (voteCount > 0) "★ " + String.format(Locale.US, "%.1f", voteAverage) else null

/** Taste-match chip: a small green dot plus "92%". Shown on artwork, so colours are fixed. */
@Composable
fun MatchBadge(match: Int, modifier: Modifier = Modifier, large: Boolean = false) {
    val brand = Brand.tokens
    Surface(modifier = modifier, shape = CircleShape, color = Color.Black.copy(alpha = 0.72f)) {
        Row(
            Modifier.padding(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 6.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(if (large) 8.dp else 6.dp).background(brand.match, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                if (large) "$match% match" else "$match%",
                style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
fun RatingPill(rating: Double, modifier: Modifier = Modifier, votes: Int? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = Brand.tokens.rating, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(String.format(Locale.US, "%.1f", rating), style = MaterialTheme.typography.labelLarge)
        if (votes != null && votes > 0) {
            Text(
                "  " + compactCount(votes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun compactCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 10_000 -> "${n / 1_000}k"
    n >= 1_000 -> String.format(Locale.US, "%.1fk", n / 1_000.0)
    else -> n.toString()
}

/** Why-it's-here label with a spark icon. Sits on artwork, so it uses fixed light-on-dark colours. */
@Composable
fun ReasonPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
        Row(Modifier.padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Brand.tokens.gradientColors.first(),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    Column(modifier.padding(horizontal = 20.dp)) {
        if (eyebrow != null) {
            Text(eyebrow.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MovieRow(
    title: String,
    items: List<RowItem>,
    section: String,
    onMovieClick: OpenMovie,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onMovieLongClick: ((Movie) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(title, eyebrow = eyebrow, subtitle = subtitle)
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.movie.id }) { _, item ->
                val key = posterKey(section, item.movie.id)
                PosterCard(
                    movie = item.movie,
                    caption = item.caption,
                    match = item.match,
                    sharedKey = key,
                    onClick = { onMovieClick(item.movie, key) },
                    onLongClick = onMovieLongClick?.let { { it(item.movie) } },
                )
            }
        }
    }
}

/** "Top 10" style row: oversized outlined rank numerals tucked behind each poster. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankedRow(
    title: String,
    items: List<RowItem>,
    section: String,
    onMovieClick: OpenMovie,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onMovieLongClick: ((Movie) -> Unit)? = null,
) {
    val numeral = TextStyle(
        fontFamily = Fraunces,
        fontSize = 120.sp,
        lineHeight = 120.sp,
        drawStyle = Stroke(width = 5f),
    )
    Column(modifier) {
        SectionHeader(title, eyebrow = eyebrow, subtitle = subtitle)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(items, key = { _, item -> item.movie.id }) { index, item ->
                val key = posterKey(section, item.movie.id)
                Box(
                    Modifier
                        .width(if (index + 1 >= 10) 196.dp else 168.dp)
                        .combinedClickable(
                            onClick = { onMovieClick(item.movie, key) },
                            onLongClick = onMovieLongClick?.let { { it(item.movie) } },
                        ),
                ) {
                    Text(
                        "${index + 1}",
                        style = numeral,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.BottomStart).offset(y = 18.dp),
                    )
                    Box(Modifier.align(Alignment.TopEnd).width(118.dp).shadow(12.dp, PosterShape)) {
                        PosterImage(item.movie.posterPath, contentDescription = item.movie.title, sharedKey = key)
                        item.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let {
                            MatchBadge(it, Modifier.align(Alignment.BottomStart).padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Primary call to action, filled with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val brand = Brand.tokens
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(CircleShape)
            .background(if (enabled) brand.gradient else Brush.linearGradient(List(2) { MaterialTheme.colorScheme.surfaceContainerHighest }))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Secondary pill button with a tonal fill. */
@Composable
fun TonalPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = contentDescription)
                if (text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Round translucent button for use over artwork (back, share, watchlist on the hero). */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            GradientButton(actionLabel, onAction)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    MessageState(Icons.Outlined.CloudOff, "Couldn't load movies", message, modifier, "Try again", onRetry)

/** A soft light sweep across a placeholder. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(Brush.linearGradient(listOf(base, highlight, base), start = Offset(x, 0f), end = Offset(x + 600f, 300f)))
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = PosterShape) {
    Box(modifier.clip(shape).shimmer())
}

@Composable
fun LoadingFeed(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.78f), shape = RoundedCornerShape(0.dp))
        repeat(2) {
            Column {
                ShimmerBox(Modifier.padding(horizontal = 20.dp).width(180.dp).height(24.dp), RoundedCornerShape(6.dp))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) { ShimmerBox(Modifier.width(132.dp).aspectRatio(2f / 3f)) }
                }
            }
        }
    }
}
