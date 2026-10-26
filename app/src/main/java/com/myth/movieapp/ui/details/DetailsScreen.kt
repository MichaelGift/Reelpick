package com.myth.movieapp.ui.details

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.CastMember
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.model.MovieDetails
import com.myth.movieapp.domain.model.WatchProvider
import com.myth.movieapp.domain.model.WatchProviders
import com.myth.movieapp.ui.common.Card
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.GlassIconButton
import com.myth.movieapp.ui.common.GradientButton
import com.myth.movieapp.ui.common.MovieRow
import com.myth.movieapp.ui.common.OpenMovie
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.common.RatingPill
import com.myth.movieapp.ui.common.RowItem
import com.myth.movieapp.ui.common.SectionHeader
import com.myth.movieapp.ui.common.ShimmerBox
import com.myth.movieapp.ui.common.TmdbImages
import com.myth.movieapp.ui.common.TonalPillButton
import com.myth.movieapp.ui.theme.Brand
import com.myth.movieapp.ui.theme.EyebrowStyle

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onMovieClick: OpenMovie,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    // Once content scrolls under the status bar, back it with a scrim so it stays legible.
    val scrimAlpha by animateFloatAsState(if (scrollState.value > 600) 0.96f else 0f, label = "statusScrim")

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val content = state.content) {
            DetailsContent.Loading -> DetailsLoading(viewModel.preview, viewModel.sharedKey, scrollState)
            is DetailsContent.Error -> ErrorState(content.message, viewModel::load)
            is DetailsContent.Loaded -> DetailsBody(
                content = content,
                state = state,
                sharedKey = viewModel.sharedKey,
                scrollState = scrollState,
                onFeedback = viewModel::toggleFeedback,
                onToggleWatchlist = viewModel::toggleWatchlist,
                onMovieClick = onMovieClick,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            (state.content as? DetailsContent.Loaded)?.details?.movie?.let { movie ->
                Text(
                    movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(4f).graphicsLayer { alpha = scrimAlpha },
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Share, "Share", onClick = { context.shareMovie(movie) })
            }
        }
    }
}

private fun android.content.Context.shareMovie(movie: Movie) {
    val text = listOfNotNull(movie.title, movie.releaseYear?.let { "($it)" }).joinToString(" ") +
        " https://www.themoviedb.org/movie/${movie.id}"
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}

/**
 * Backdrop, overlapping poster and title. Rendered from the route's preview while details load,
 * so the tapped poster has a target to morph into on the very first frame.
 */
@Composable
private fun Header(
    preview: DetailsPreview,
    sharedKey: String?,
    scrollState: ScrollState,
    meta: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(420.dp).clipToBounds()) {
            AsyncImage(
                model = TmdbImages.backdrop(preview.backdropPath) ?: TmdbImages.posterLarge(preview.posterPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = scrollState.value * 0.5f }
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.5f),
                        0.25f to Color.Transparent,
                        0.6f to MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
            )
        }
        // The poster overlaps the bottom of the artwork.
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 300.dp), verticalAlignment = Alignment.Bottom) {
            PosterImage(
                preview.posterPath,
                contentDescription = preview.title,
                sharedKey = sharedKey,
                modifier = Modifier.width(118.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(preview.title, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                meta()
            }
        }
    }
}

@Composable
private fun DetailsLoading(preview: DetailsPreview, sharedKey: String?, scrollState: ScrollState) {
    Column(Modifier.verticalScroll(scrollState)) {
        Header(preview, sharedKey, scrollState) {
            ShimmerBox(Modifier.width(120.dp).height(16.dp), RoundedCornerShape(6.dp))
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBox(Modifier.fillMaxWidth().height(92.dp), MaterialTheme.shapes.large)
            ShimmerBox(Modifier.fillMaxWidth().height(54.dp), CircleShape)
            ShimmerBox(Modifier.fillMaxWidth().height(52.dp), CircleShape)
            repeat(3) { ShimmerBox(Modifier.fillMaxWidth().height(14.dp), RoundedCornerShape(6.dp)) }
        }
    }
}

@Composable
private fun DetailsBody(
    content: DetailsContent.Loaded,
    state: DetailsUiState,
    sharedKey: String?,
    scrollState: ScrollState,
    onFeedback: (FeedbackType) -> Unit,
    onToggleWatchlist: () -> Unit,
    onMovieClick: OpenMovie,
) {
    val details = content.details
    val movie = details.movie
    val context = LocalContext.current

    Column(Modifier.verticalScroll(scrollState).navigationBarsPadding().padding(bottom = 24.dp)) {
        Header(
            preview = DetailsPreview(movie.title, movie.posterPath, movie.backdropPath),
            sharedKey = sharedKey,
            scrollState = scrollState,
        ) {
            Text(
                listOfNotNull(movie.releaseYear, details.runtimeMinutes?.let(::formatRuntime)).joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (movie.voteCount > 0) {
                Spacer(Modifier.height(6.dp))
                RatingPill(movie.voteAverage, votes = movie.voteCount)
            }
        }

        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GenreTags(details)
                content.fit?.let { TasteFitCard(it, Modifier.padding(horizontal = 20.dp)) }
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton(
                        text = if (state.inWatchlist) "On your watchlist" else "Add to watchlist",
                        onClick = onToggleWatchlist,
                        icon = if (state.inWatchlist) Icons.Rounded.Check else Icons.Rounded.Add,
                        modifier = Modifier.weight(1f),
                    )
                    details.trailerYoutubeKey?.let { key ->
                        TonalPillButton(
                            text = "Trailer",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$key".toUri()))
                            },
                        )
                    }
                }
                RatingSegments(state.feedback, onFeedback, Modifier.padding(horizontal = 20.dp))
            }

            Synopsis(details, Modifier.padding(horizontal = 20.dp))

            WhereToWatch(details.watchProviders, content.region, Modifier.padding(horizontal = 20.dp)) { link ->
                context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            }

            if (details.cast.isNotEmpty()) CastRow(details.cast)

            if (content.moreLikeThis.isNotEmpty()) {
                MovieRow(
                    title = "More like this",
                    eyebrow = "If you like ${movie.title}",
                    items = content.moreLikeThis.map { RowItem(it) },
                    section = "similar-${movie.id}",
                    onMovieClick = onMovieClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreTags(details: MovieDetails) {
    if (details.genres.isEmpty()) return
    FlowRow(
        Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        details.genres.forEach {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(it.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun TasteFitCard(fit: TasteFit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(Brand.tokens.gradient, CircleShape), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${fit.match}%", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("TASTE MATCH", style = EyebrowStyle, color = Brand.tokens.match)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (fit.sharedGenres.isEmpty()) "Close to what you usually enjoy"
                    else "You tend to love ${fit.sharedGenres.naturalJoin()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Like / Not for me / Seen as one segmented control; tapping the active segment clears it. */
@Composable
private fun RatingSegments(current: FeedbackType?, onFeedback: (FeedbackType) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Surface(modifier.fillMaxWidth(), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(4.dp)) {
            Segment("Like", "Like", current == FeedbackType.LIKE, Icons.Rounded.ThumbUp, Icons.Outlined.ThumbUp) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFeedback(FeedbackType.LIKE)
            }
            Segment("Not for me", "Dislike", current == FeedbackType.DISLIKE, Icons.Rounded.Block, Icons.Outlined.Block) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFeedback(FeedbackType.DISLIKE)
            }
            Segment("Seen it", "Watched", current == FeedbackType.WATCHED, Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle) {
                onFeedback(FeedbackType.WATCHED)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(
    label: String,
    description: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .weight(1f)
            .height(44.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
                role = Role.Tab
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Synopsis(details: MovieDetails, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.animateContentSize()) {
        details.tagline?.let {
            Text("“$it”", style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.myth.movieapp.ui.theme.Fraunces))
            Spacer(Modifier.height(10.dp))
        }
        Text(
            details.movie.overview.ifBlank { "No synopsis available." },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (details.movie.overview.length > 220) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Show less" else "Read more")
            }
        }
    }
}

@Composable
private fun WhereToWatch(providers: WatchProviders?, region: String, modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 18.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Where to watch", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(region, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (providers == null) {
                Text(
                    "Not streaming, renting or selling in your region right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            } else {
                ProviderGroup("Stream", providers.stream)
                ProviderGroup("Rent", providers.rent)
                ProviderGroup("Buy", providers.buy)
                providers.link?.let { link ->
                    TextButton(onClick = { onOpen(link) }, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text("See all options")
                    }
                }
            }
            // Required attribution for watch-provider data.
            Text(
                "Availability data by JustWatch",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProviderGroup(title: String, providers: List<WatchProvider>) {
    if (providers.isEmpty()) return
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(
            title.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(providers, key = { it.id }) { provider ->
                Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = TmdbImages.logo(provider.logoPath),
                        contentDescription = provider.name,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        provider.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Spacer(Modifier.height(14.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(cast, key = { it.id }) { member ->
                Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            AsyncImage(
                                model = TmdbImages.profile(member.profilePath),
                                contentDescription = member.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(member.name, style = MaterialTheme.typography.labelLarge, maxLines = 2, textAlign = TextAlign.Center)
                    member.character?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
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

/** "A", "A and B", "A, B and C". */
private fun List<String>.naturalJoin(): String =
    if (size <= 2) joinToString(" and ") else dropLast(1).joinToString(", ") + " and " + last()

private fun formatRuntime(minutes: Int): String = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
