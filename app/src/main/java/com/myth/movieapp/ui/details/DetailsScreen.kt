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
import com.myth.movieapp.domain.model.CrewMember
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Title
import com.myth.movieapp.domain.model.TitleDetails
import com.myth.movieapp.domain.model.TvInfo
import com.myth.movieapp.domain.model.WatchProvider
import com.myth.movieapp.domain.model.WatchProviders
import com.myth.movieapp.ui.common.Card
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.GlassIconButton
import com.myth.movieapp.ui.common.GradientButton
import com.myth.movieapp.ui.common.TitleRow
import com.myth.movieapp.ui.common.OpenPerson
import com.myth.movieapp.ui.common.OpenTitle
import com.myth.movieapp.ui.common.PersonAvatar
import com.myth.movieapp.ui.common.personKey
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
    onMovieClick: OpenTitle,
    onPersonClick: OpenPerson,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    // Once content scrolls under the status bar, back it with a scrim so it stays legible.
    val scrimAlpha by animateFloatAsState(if (scrollState.value > 600) 1f else 0f, label = "statusScrim")

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
                onPersonClick = onPersonClick,
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
            (state.content as? DetailsContent.Loaded)?.details?.title?.let { movie ->
                Text(
                    movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(4f).graphicsLayer { alpha = scrimAlpha },
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Share, "Share", onClick = { context.shareTitle(movie) })
            }
        }
    }
}

private fun android.content.Context.shareTitle(movie: Title) {
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
    onMovieClick: OpenTitle,
    onPersonClick: OpenPerson,
) {
    val details = content.details
    val movie = details.title
    val context = LocalContext.current

    Column(Modifier.verticalScroll(scrollState).navigationBarsPadding().padding(bottom = 24.dp)) {
        Header(
            preview = DetailsPreview(movie.title, movie.posterPath, movie.backdropPath),
            sharedKey = sharedKey,
            scrollState = scrollState,
        ) {
            Text(
                details.tv?.let(::seriesMeta)
                    ?: listOfNotNull(movie.releaseYear, details.runtimeMinutes?.let(::formatRuntime)).joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (movie.voteCount > 0) {
                Spacer(Modifier.height(6.dp))
                RatingPill(movie.voteAverage, votes = movie.voteCount)
            }
            details.tv?.let { tv ->
                Spacer(Modifier.height(8.dp))
                SeriesStatus(tv)
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

            details.tv?.takeIf { it.seasons.isNotEmpty() }?.let { SeasonsRow(it, fallbackPoster = movie.posterPath) }

            WhereToWatch(details.watchProviders, content.region, Modifier.padding(horizontal = 20.dp)) { link ->
                context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            }

            val crew = details.tv?.creators ?: details.crew
            if (crew.isNotEmpty()) {
                CrewRow(
                    title = if (details.tv != null) "Created by" else "Behind the camera",
                    crew = crew,
                    onPersonClick = onPersonClick,
                )
            }

            if (details.cast.isNotEmpty()) CastRow(details.cast, onPersonClick)

            if (content.moreLikeThis.isNotEmpty()) {
                TitleRow(
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
private fun GenreTags(details: TitleDetails) {
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
private fun Synopsis(details: TitleDetails, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.animateContentSize()) {
        details.tagline?.let {
            Text("“$it”", style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.myth.movieapp.ui.theme.Fraunces))
            Spacer(Modifier.height(10.dp))
        }
        Text(
            details.title.overview.ifBlank { "No synopsis available." },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (details.title.overview.length > 220) {
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
private fun CastRow(cast: List<CastMember>, onPersonClick: OpenPerson) {
    Column {
        SectionHeader("Cast")
        Spacer(Modifier.height(14.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(cast, key = { it.id }) { member ->
                val key = personKey("cast", member.id)
                Column(
                    Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPersonClick(member.id, member.name, member.profilePath, key) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PersonAvatar(member.profilePath, contentDescription = member.name, size = 80.dp, sharedKey = key)
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
                    member.episodeCount?.let {
                        Text(
                            plural(it, "episode"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** Directors and writers for a film, creators for a series: each opens their page. */
@Composable
private fun CrewRow(title: String, crew: List<CrewMember>, onPersonClick: OpenPerson) {
    Column {
        SectionHeader(title)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(crew, key = { it.id }) { member ->
                val key = personKey("crew", member.id)
                Surface(
                    onClick = { onPersonClick(member.id, member.name, member.profilePath, key) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PersonAvatar(member.profilePath, contentDescription = null, size = 40.dp, sharedKey = key)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(member.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                member.job,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "A", "A and B", "A, B and C". */
private fun List<String>.naturalJoin(): String =
    if (size <= 2) joinToString(" and ") else dropLast(1).joinToString(", ") + " and " + last()

/** "2011–2019 · 8 seasons · 73 episodes", or "2019– · ..." while still running. */
private fun seriesMeta(tv: TvInfo): String = listOfNotNull(
    tv.firstAirYear?.let { first ->
        when {
            tv.inProduction -> "$first–"
            tv.lastAirYear == null || tv.lastAirYear == first -> first
            else -> "$first–${tv.lastAirYear}"
        }
    },
    plural(tv.seasonCount, "season").takeIf { tv.seasonCount > 0 },
    plural(tv.episodeCount, "episode").takeIf { tv.episodeCount > 0 },
).joinToString("  ·  ")

private fun plural(count: Int, noun: String) = "$count ${if (count == 1) noun else noun + "s"}"

/** Status pill ("Returning series" / "Ended") plus the networks it airs on. */
@Composable
private fun SeriesStatus(tv: TvInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        tv.status?.let { status ->
            val returning = tv.inProduction || status.startsWith("Returning", ignoreCase = true)
            Surface(
                shape = CircleShape,
                color = if (returning) Brand.tokens.match.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    if (returning) "Returning series" else status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (returning) Brand.tokens.match else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (tv.networks.isNotEmpty()) {
            Text(
                tv.networks.joinToString(" · ") { it.name },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeasonsRow(tv: TvInfo, fallbackPoster: String?) {
    Column {
        SectionHeader("Seasons", subtitle = listOf(plural(tv.seasonCount, "season"), plural(tv.episodeCount, "episode")).joinToString("  ·  "))
        Spacer(Modifier.height(14.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(tv.seasons, key = { it.number }) { season ->
                Column(Modifier.width(112.dp)) {
                    PosterImage(season.posterPath ?: fallbackPoster, contentDescription = season.name)
                    Spacer(Modifier.height(8.dp))
                    Text(season.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(if (season.episodeCount > 0) plural(season.episodeCount, "episode") else "Announced", season.airDate?.take(4)).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun formatRuntime(minutes: Int): String = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
