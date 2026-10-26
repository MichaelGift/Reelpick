package com.myth.movieapp.ui.foryou

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.recommend.Recommendation
import com.myth.movieapp.domain.recommend.TasteSummary
import com.myth.movieapp.ui.common.Card
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.GradientButton
import com.myth.movieapp.ui.common.LoadingFeed
import com.myth.movieapp.ui.common.MIN_DISPLAY_MATCH
import com.myth.movieapp.ui.common.MatchBadge
import com.myth.movieapp.ui.common.MessageState
import com.myth.movieapp.ui.common.MovieActionsSheet
import com.myth.movieapp.ui.common.MovieRow
import com.myth.movieapp.ui.common.OpenMovie
import com.myth.movieapp.ui.common.RankedRow
import com.myth.movieapp.ui.common.ReasonPill
import com.myth.movieapp.ui.common.RowItem
import com.myth.movieapp.ui.common.TmdbImages
import com.myth.movieapp.ui.common.TonalPillButton
import com.myth.movieapp.ui.common.Wordmark
import com.myth.movieapp.ui.common.label
import com.myth.movieapp.ui.common.ratingLabel
import com.myth.movieapp.ui.theme.Brand
import com.myth.movieapp.ui.theme.EyebrowStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    onMovieClick: OpenMovie,
    viewModel: ForYouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var actionsFor by remember { mutableStateOf<Movie?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.error, state.notice, state.feed) {
        val message = state.notice ?: state.error?.takeIf { state.feed != null }
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val feed = state.feed
        when {
            feed == null && state.isLoading -> LoadingFeed()
            feed == null -> ErrorState(state.error ?: "No feed yet.", viewModel::retry)
            feed.isEmpty -> MessageState(
                icon = Icons.Outlined.AutoAwesome,
                title = "You've seen it all",
                message = "You've rated everything we found. Pull to refresh, or like a few more films from Search.",
                actionLabel = "Refresh",
                onAction = viewModel::refresh,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                FeedContent(
                    feed = feed,
                    listState = listState,
                    watchlistIds = state.watchlistIds,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = { actionsFor = it },
                    onToggleWatchlist = viewModel::toggleWatchlist,
                )
            }
        }
        TopBar(listState)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    actionsFor?.let { movie ->
        MovieActionsSheet(
            movie = movie,
            onDismiss = { actionsFor = null },
            onFeedback = { viewModel.onFeedback(movie, it) },
            onAddToWatchlist = { viewModel.addToWatchlist(movie) },
        )
    }
}

/** Transparent over the hero, fading to a solid bar once the hero scrolls away. */
@Composable
private fun TopBar(listState: LazyListState) {
    val solid by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 600 }
    }
    val background by animateColorAsState(
        if (solid) MaterialTheme.colorScheme.background else Color.Transparent,
        label = "topBar",
    )
    Box(Modifier.fillMaxWidth().background(background).statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Wordmark(markSize = 26.dp, color = if (solid) MaterialTheme.colorScheme.onBackground else Color.White)
    }
}

@Composable
private fun FeedContent(
    feed: ForYouFeed,
    listState: LazyListState,
    watchlistIds: Set<Int>,
    onMovieClick: OpenMovie,
    onMovieLongClick: (Movie) -> Unit,
    onToggleWatchlist: (Movie) -> Unit,
) {
    val hero = feed.topPicks.take(HERO_COUNT)
    BoxWithConstraints {
        val heroHeight = (maxHeight * 0.72f).coerceIn(480.dp, 680.dp)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            if (hero.isNotEmpty()) {
                item(key = "hero") {
                    HeroPager(
                        picks = hero,
                        genreNames = feed.genreNames,
                        height = heroHeight,
                        listState = listState,
                        watchlistIds = watchlistIds,
                        onOpen = { onMovieClick(it, null) },
                        onLongPress = onMovieLongClick,
                        onToggleWatchlist = onToggleWatchlist,
                    )
                }
            }
            item(key = "top10") {
                RankedRow(
                    title = "Top 10 for you",
                    eyebrow = "Ranked by your taste",
                    items = feed.topPicks.take(10).map { RowItem(it.movie, match = it.match) },
                    section = "top10",
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                )
            }
            feed.taste?.takeIf { it.topGenres.isNotEmpty() }?.let { taste ->
                item(key = "taste") { TasteCard(taste, Modifier.padding(horizontal = 20.dp)) }
            }
            val matchById = feed.topPicks.associate { it.movie.id to it.match }
            feed.becauseYouLiked.forEach { row ->
                item(key = "seed-${row.seed.id}") {
                    MovieRow(
                        title = row.seed.title,
                        eyebrow = "Because you liked",
                        items = row.items.map { RowItem(it.movie, match = it.match) },
                        section = "seed-${row.seed.id}",
                        onMovieClick = onMovieClick,
                        onMovieLongClick = onMovieLongClick,
                    )
                }
            }
            feed.genreRows.forEach { row ->
                item(key = "genre-${row.genre.id}") {
                    MovieRow(
                        title = row.genre.name,
                        eyebrow = "One of your genres",
                        items = row.items.map { RowItem(it.movie, match = it.match) },
                        section = "genre-${row.genre.id}",
                        onMovieClick = onMovieClick,
                        onMovieLongClick = onMovieLongClick,
                    )
                }
            }
            if (feed.trending.isNotEmpty()) {
                item(key = "trending") {
                    MovieRow(
                        title = "Trending this week",
                        eyebrow = "Everyone's watching",
                        items = feed.trending.map { RowItem(it, match = matchById[it.id]) },
                        section = "trending",
                        onMovieClick = onMovieClick,
                        onMovieLongClick = onMovieLongClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(
    picks: List<Recommendation>,
    genreNames: Map<Int, String>,
    height: Dp,
    listState: LazyListState,
    watchlistIds: Set<Int>,
    onOpen: (Movie) -> Unit,
    onLongPress: (Movie) -> Unit,
    onToggleWatchlist: (Movie) -> Unit,
) {
    val pagerState = rememberPagerState { picks.size }
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Gentle auto-advance; any touch restarts the timer.
    LaunchedEffect(pagerState.currentPage, dragged) {
        if (dragged) return@LaunchedEffect
        delay(7_000)
        pagerState.animateScrollToPage((pagerState.currentPage + 1) % picks.size)
    }

    // Clip so the parallax-shifted artwork never spills below the hero.
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val pick = picks[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .combinedClickable(onClick = { onOpen(pick.movie) }, onLongClick = { onLongPress(pick.movie) }),
            ) {
                AsyncImage(
                    // Backdrops carry no baked-in titles, so they sit cleanly under our own type.
                    model = TmdbImages.backdrop(pick.movie.backdropPath) ?: TmdbImages.posterLarge(pick.movie.posterPath),
                    contentDescription = pick.movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Parallax: the artwork scrolls at half speed.
                            if (listState.firstVisibleItemIndex == 0) translationY = listState.firstVisibleItemScrollOffset * 0.5f
                        },
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.18f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.78f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                HeroCopy(
                    pick = pick,
                    genreNames = genreNames,
                    inWatchlist = pick.movie.id in watchlistIds,
                    onOpen = { onOpen(pick.movie) },
                    onToggleWatchlist = { onToggleWatchlist(pick.movie) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
                )
            }
        }
        PageDots(picks.size, pagerState.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun HeroCopy(
    pick: Recommendation,
    genreNames: Map<Int, String>,
    inWatchlist: Boolean,
    onOpen: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = pick.movie
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            pick.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let {
                MatchBadge(it, large = true)
                Spacer(Modifier.width(8.dp))
            }
            ReasonPill(pick.reason.label(), Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            movie.title,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            listOfNotNull(
                movie.releaseYear,
                movie.genreIds.mapNotNull { genreNames[it] }.take(2).joinToString(", ").ifEmpty { null },
                movie.ratingLabel(),
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("More info", onOpen, Modifier.weight(1f), icon = Icons.Outlined.Info)
            TonalPillButton(
                text = "",
                onClick = onToggleWatchlist,
                icon = if (inWatchlist) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (inWatchlist) "Remove from watchlist" else "Add to watchlist",
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val width by animateDpAsState(if (index == current) 22.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (index == current) Brand.tokens.gradient
                        else Brush.linearGradient(List(2) { MaterialTheme.colorScheme.outline }),
                    ),
            )
        }
    }
}

/** Shows the learned genre weights back to the user: the feed is explainable, not a black box. */
@Composable
private fun TasteCard(taste: TasteSummary, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("YOUR TASTE PROFILE", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text("What shapes your picks", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            val max = taste.topGenres.maxOf { it.weight }.coerceAtLeast(0.0001)
            taste.topGenres.forEach { genre ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(genre.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(112.dp), maxLines = 1)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((genre.weight / max).toFloat().coerceIn(0.04f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Brand.tokens.gradient),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                buildList {
                    add("${taste.likes} ${if (taste.likes == 1) "like" else "likes"}")
                    if (taste.dislikes > 0) add("${taste.dislikes} passed")
                    if (taste.saved > 0) add("${taste.saved} saved")
                }.joinToString("  ·  ") + "  ·  long-press any poster to teach it more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val HERO_COUNT = 5
