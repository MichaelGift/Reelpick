package com.myth.movieapp.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.Movie
import java.util.Locale

@Composable
fun PosterImage(path: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(
            model = TmdbImages.poster(path),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 128.dp,
    caption: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            PosterImage(movie.posterPath, contentDescription = movie.title)
            if (movie.voteCount > 0) {
                RatingBadge(movie.voteAverage, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(movie.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            caption ?: movie.releaseYear.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text(String.format(Locale.US, "%.1f", rating), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    captionFor: (Movie) -> String? = { null },
    onMovieLongClick: ((Movie) -> Unit)? = null,
) {
    Column(modifier) {
        SectionHeader(title, subtitle = subtitle)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                PosterCard(
                    movie = movie,
                    caption = captionFor(movie),
                    onClick = { onMovieClick(movie) },
                    onLongClick = onMovieLongClick?.let { { it(movie) } },
                )
            }
        }
    }
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
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    MessageState(Icons.Outlined.CloudOff, "Couldn't load movies", message, modifier, "Retry", onRetry)

/** Pulsing placeholder used while content loads. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Box(
        modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun LoadingRows(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(220.dp))
        repeat(rows) {
            Column {
                ShimmerBox(Modifier.padding(horizontal = 16.dp).width(160.dp).height(18.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { ShimmerBox(Modifier.width(128.dp).aspectRatio(2f / 3f)) }
                }
            }
        }
    }
}
