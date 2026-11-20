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
import androidx.compose.material.icons.rounded.Person
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
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Title
import com.myth.core.design.CoverImage
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.core.design.MatchBadge
import com.myth.core.design.MessageState
import com.myth.core.design.PosterShape
import com.myth.core.design.SectionHeader
import com.myth.core.design.sharedPoster
import com.myth.core.design.Fraunces
import java.util.Locale

/** One poster in a row, with optional caption and taste match. */
data class RowItem(val movie: Title, val caption: String? = null, val match: Int? = null)

@Composable
fun PosterImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    shape: Shape = PosterShape,
    showPlaceholderIcon: Boolean = true,
) = CoverImage(
    model = TmdbImages.poster(path),
    contentDescription = contentDescription,
    modifier = modifier,
    sharedKey = sharedKey,
    shape = shape,
    placeholder = if (showPlaceholderIcon) Icons.Outlined.Movie else null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    movie: Title,
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
            if (movie.mediaType == MediaType.TV) SeriesBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
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

fun Title.ratingLabel(): String? = if (voteCount > 0) "★ " + String.format(Locale.US, "%.1f", voteAverage) else null

/** Round headshot with a placeholder; [sharedKey] lets it morph into the person page header. */
@Composable
fun PersonAvatar(
    profilePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    sharedKey: String? = null,
) {
    Box(
        modifier
            .size(size)
            .sharedPoster(sharedKey)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        AsyncImage(
            model = TmdbImages.profile(profilePath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Marks a poster as a TV series so mixed rows stay readable. Sits on artwork: fixed colours. */
@Composable
fun SeriesBadge(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.72f)) {
        Text(
            "SERIES",
            style = EyebrowStyle.copy(fontSize = 9.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun TitleRow(
    title: String,
    items: List<RowItem>,
    section: String,
    onMovieClick: OpenTitle,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onMovieLongClick: ((Title) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(title, eyebrow = eyebrow, subtitle = subtitle)
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.movie.stableKey }) { _, item ->
                val key = posterKey(section, item.movie)
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
    onMovieClick: OpenTitle,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onMovieLongClick: ((Title) -> Unit)? = null,
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
            itemsIndexed(items, key = { _, item -> item.movie.stableKey }) { index, item ->
                val key = posterKey(section, item.movie)
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
                        if (item.movie.mediaType == MediaType.TV) {
                            SeriesBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
                        }
                        item.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let {
                            MatchBadge(it, Modifier.align(Alignment.BottomStart).padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    MessageState(Icons.Outlined.CloudOff, "Couldn't load movies", message, modifier, "Try again", onRetry)
