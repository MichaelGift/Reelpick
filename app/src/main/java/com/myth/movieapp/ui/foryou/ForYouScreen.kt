package com.myth.movieapp.ui.foryou

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.domain.recommend.ForYouFeed
import com.myth.movieapp.domain.recommend.Recommendation
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.LoadingRows
import com.myth.movieapp.ui.common.MessageState
import com.myth.movieapp.ui.common.MovieActionsSheet
import com.myth.movieapp.ui.common.MovieRow
import com.myth.movieapp.ui.common.RatingBadge
import com.myth.movieapp.ui.common.TmdbImages
import com.myth.movieapp.ui.common.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: ForYouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var actionsFor by remember { mutableStateOf<Movie?>(null) }

    LaunchedEffect(state.error, state.feed) {
        val error = state.error
        if (error != null && state.feed != null) {
            snackbar.showSnackbar(error)
            viewModel.errorShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("For you") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val feed = state.feed
        when {
            feed == null && state.isLoading -> LoadingRows(Modifier.padding(padding))
            feed == null -> ErrorState(state.error ?: "No feed yet.", viewModel::retry, Modifier.padding(padding))
            feed.isEmpty -> MessageState(
                icon = Icons.Outlined.AutoAwesome,
                title = "Nothing new to suggest",
                message = "You've rated everything we found. Pull to refresh or like a few more movies from Search.",
                modifier = Modifier.padding(padding),
                actionLabel = "Refresh",
                onAction = viewModel::refresh,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                FeedContent(feed, onMovieClick = { onMovieClick(it.id) }, onMovieLongClick = { actionsFor = it })
            }
        }
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

@Composable
private fun FeedContent(feed: ForYouFeed, onMovieClick: (Movie) -> Unit, onMovieLongClick: (Movie) -> Unit) {
    val hero = feed.topPicks.take(5)
    val reasons = feed.topPicks.associate { it.movie.id to it.reason.label() }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (hero.isNotEmpty()) {
            item(key = "hero") { HeroCarousel(hero, onMovieClick, onMovieLongClick) }
        }
        if (feed.topPicks.size > hero.size) {
            item(key = "top") {
                MovieRow(
                    title = "Top picks for you",
                    subtitle = "Long-press any poster to rate it",
                    movies = feed.topPicks.drop(hero.size).map { it.movie },
                    captionFor = { reasons[it.id] },
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                )
            }
        }
        feed.becauseYouLiked.forEach { row ->
            item(key = "seed-${row.seed.id}") {
                MovieRow(
                    title = "Because you liked ${row.seed.title}",
                    movies = row.items.map { it.movie },
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                )
            }
        }
        feed.genreRows.forEach { row ->
            item(key = "genre-${row.genre.id}") {
                MovieRow(
                    title = "Your genres: ${row.genre.name}",
                    movies = row.items.map { it.movie },
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                )
            }
        }
        if (feed.trending.isNotEmpty()) {
            item(key = "trending") {
                MovieRow(
                    title = "Trending this week",
                    movies = feed.trending,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    picks: List<Recommendation>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
) {
    val pagerState = rememberPagerState { picks.size }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.padding(top = 8.dp),
    ) { page ->
        val pick = picks[page]
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = { onMovieClick(pick.movie) },
                    onLongClick = { onMovieLongClick(pick.movie) },
                ),
        ) {
            AsyncImage(
                model = TmdbImages.backdrop(pick.movie.backdropPath ?: pick.movie.posterPath),
                contentDescription = pick.movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                AssistChip(onClick = { onMovieClick(pick.movie) }, label = { Text(pick.reason.label(), maxLines = 1) })
                Spacer(Modifier.height(4.dp))
                Text(
                    pick.movie.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(pick.movie.releaseYear).joinToString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            if (pick.movie.voteCount > 0) {
                RatingBadge(pick.movie.voteAverage, Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
        }
    }
}
