package com.myth.movieapp.ui.details

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.CastMember
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.WatchProvider
import com.myth.movieapp.domain.model.WatchProviders
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.MovieRow
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.common.RatingBadge
import com.myth.movieapp.ui.common.SectionHeader
import com.myth.movieapp.ui.common.TmdbImages

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val content = state.content) {
            DetailsContent.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is DetailsContent.Error -> ErrorState(content.message, viewModel::load)
            is DetailsContent.Loaded -> DetailsBody(
                content = content,
                state = state,
                onFeedback = viewModel::toggleFeedback,
                onToggleWatchlist = viewModel::toggleWatchlist,
                onMovieClick = onMovieClick,
            )
        }
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 12.dp, top = 40.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            ),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
private fun DetailsBody(
    content: DetailsContent.Loaded,
    state: DetailsUiState,
    onFeedback: (FeedbackType) -> Unit,
    onToggleWatchlist: () -> Unit,
    onMovieClick: (Int) -> Unit,
) {
    val details = content.details
    val movie = details.movie
    val context = LocalContext.current

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Box {
            AsyncImage(
                model = TmdbImages.backdrop(movie.backdropPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.5f to Color.Transparent, 1f to MaterialTheme.colorScheme.background),
                ),
            )
        }
        Row(Modifier.padding(horizontal = 16.dp).offset(y = (-48).dp)) {
            PosterImage(movie.posterPath, contentDescription = movie.title, modifier = Modifier.width(112.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.padding(top = 52.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(movie.releaseYear, details.runtimeMinutes?.let(::formatRuntime)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (movie.voteCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    RatingBadge(movie.voteAverage)
                }
            }
        }

        Column(Modifier.offset(y = (-32).dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            GenreChips(details)
            ActionRow(state, onFeedback, onToggleWatchlist)

            details.trailerYoutubeKey?.let { key ->
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$key".toUri()))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Watch trailer")
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                details.tagline?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                Text(movie.overview.ifBlank { "No overview available." }, style = MaterialTheme.typography.bodyMedium)
            }

            WhereToWatch(details.watchProviders, content.region) { link ->
                context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            }

            if (details.cast.isNotEmpty()) CastRow(details.cast)

            if (content.moreLikeThis.isNotEmpty()) {
                MovieRow(
                    title = "More like this",
                    movies = content.moreLikeThis,
                    onMovieClick = { onMovieClick(it.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(details: MovieDetails) {
    if (details.genres.isEmpty()) return
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        details.genres.forEach { AssistChip(onClick = {}, label = { Text(it.name) }) }
    }
}

@Composable
private fun ActionRow(state: DetailsUiState, onFeedback: (FeedbackType) -> Unit, onToggleWatchlist: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToggleAction("Like", state.feedback == FeedbackType.LIKE, Icons.Rounded.ThumbUp, Icons.Outlined.ThumbUp) {
            onFeedback(FeedbackType.LIKE)
        }
        ToggleAction("Dislike", state.feedback == FeedbackType.DISLIKE, Icons.Rounded.ThumbDown, Icons.Outlined.ThumbDown) {
            onFeedback(FeedbackType.DISLIKE)
        }
        ToggleAction("Watchlist", state.inWatchlist, Icons.Outlined.Bookmark, Icons.Outlined.BookmarkBorder, onToggleWatchlist)
        ToggleAction("Watched", state.feedback == FeedbackType.WATCHED, Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle) {
            onFeedback(FeedbackType.WATCHED)
        }
    }
}

@Composable
private fun ToggleAction(
    label: String,
    checked: Boolean,
    checkedIcon: ImageVector,
    uncheckedIcon: ImageVector,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconToggleButton(checked = checked, onCheckedChange = { onClick() }) {
            Icon(if (checked) checkedIcon else uncheckedIcon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WhereToWatch(providers: WatchProviders?, region: String, onOpen: (String) -> Unit) {
    Column {
        SectionHeader("Where to watch", subtitle = "Region: $region")
        Spacer(Modifier.height(8.dp))
        if (providers == null) {
            Text(
                "No streaming, rental or purchase options listed for this region.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            ProviderGroup("Stream", providers.stream)
            ProviderGroup("Rent", providers.rent)
            ProviderGroup("Buy", providers.buy)
            providers.link?.let { link ->
                TextButton(onClick = { onOpen(link) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("See all options on TMDB")
                }
            }
        }
        // Required attribution for watch-provider data.
        Text(
            "Streaming availability provided by JustWatch.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ProviderGroup(title: String, providers: List<WatchProvider>) {
    if (providers.isEmpty()) return
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(providers, key = { it.id }) { provider ->
                Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = TmdbImages.logo(provider.logoPath),
                        contentDescription = provider.name,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Text(
                        provider.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CastRow(cast: List<CastMember>) {
    Column {
        SectionHeader("Cast")
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cast, key = { it.id }) { member ->
                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            AsyncImage(
                                model = TmdbImages.profile(member.profilePath),
                                contentDescription = member.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Text(member.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center)
                    member.character?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun formatRuntime(minutes: Int): String = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
