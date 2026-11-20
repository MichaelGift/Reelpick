package com.myth.playpick.ui.foryou

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.Brand
import com.myth.core.design.Card
import com.myth.core.design.CoverImage
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.GradientButton
import com.myth.core.design.LoadingFeed
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.core.design.MatchBadge
import com.myth.core.design.MessageState
import com.myth.core.design.ReasonPill
import com.myth.core.design.TonalPillButton
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.domain.recommend.GameFeed
import com.myth.playpick.domain.recommend.Pick
import com.myth.playpick.domain.recommend.TasteSummary
import com.myth.playpick.domain.recommend.Weighted
import com.myth.playpick.ui.common.GameActionsSheet
import com.myth.playpick.ui.common.GameRow
import com.myth.playpick.ui.common.GameRowItem
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RankedGameRow
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.Wordmark
import com.myth.playpick.ui.common.gameKey
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(onOpen: OpenGame, viewModel: ForYouViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetFor by remember { mutableStateOf<Game?>(null) }
    val feed = state.feed

    when {
        feed == null && state.isLoading -> LoadingFeed(Modifier.statusBarsPadding())
        feed == null -> MessageState(Icons.Outlined.CloudOff, "Couldn't load games", state.error.orEmpty(), actionLabel = "Try again", onAction = viewModel::refresh)
        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            Feed(feed, state, onOpen, onLongClick = { sheetFor = it }, onScope = viewModel::setScope, onSave = { viewModel.addToLibrary(it, LibraryStatus.BACKLOG) })
        }
    }

    sheetFor?.let { game ->
        GameActionsSheet(
            game = game,
            onDismiss = { sheetFor = null },
            onFeedback = { viewModel.rate(game, it); sheetFor = null },
            onStatus = { viewModel.addToLibrary(game, it); sheetFor = null },
        )
    }
}

private fun List<Pick>.rowItems() = map { GameRowItem(it.game, match = it.match) }

@Composable
private fun Feed(
    feed: GameFeed,
    state: ForYouUiState,
    onOpen: OpenGame,
    onLongClick: (Game) -> Unit,
    onScope: (Platform?) -> Unit,
    onSave: (Game) -> Unit,
) {
    val list = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
        item(key = "hero") {
            Box {
                if (feed.topPicks.isNotEmpty()) Hero(feed.topPicks.take(HERO_COUNT), onOpen, onSave)
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Wordmark(color = Color.White)
                    if (state.platforms.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        PlatformSwitch(state.platforms, state.scope, onScope)
                    }
                }
            }
        }
        if (feed.isEmpty) {
            item { Text("Nothing matches on this platform yet. Try another, or pull to refresh.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (feed.topPicks.isNotEmpty()) {
            item(key = "top") { RankedGameRow("Top 10 for you", feed.topPicks.rowItems(), "top", onOpen, eyebrow = "Ranked by your taste", onLongClick = onLongClick) }
        }
        feed.taste?.takeIf { it.genres.isNotEmpty() || it.tags.isNotEmpty() }?.let { taste ->
            item(key = "taste") { TasteCard(taste) }
        }
        feed.becauseYouLiked.forEach { row ->
            item(key = "seed-${row.seed.id}") {
                GameRow("Because you liked ${row.seed.name}", row.picks.rowItems(), "seed-${row.seed.id}", onOpen, eyebrow = "More like it", onLongClick = onLongClick)
            }
        }
        if (feed.shortAndSweet.isNotEmpty()) {
            item(key = "short") { GameRow("Short & sweet", feed.shortAndSweet.rowItems(), "short", onOpen, subtitle = "Strong matches you can finish in under ten hours", onLongClick = onLongClick) }
        }
        feed.genreRows.forEach { row ->
            item(key = "genre-${row.genre.id}") { GameRow(row.genre.name, row.picks.rowItems(), "genre-${row.genre.id}", onOpen, eyebrow = "Your genres", onLongClick = onLongClick) }
        }
        if (feed.trending.isNotEmpty()) {
            item(key = "trending") { GameRow("Popular right now", feed.trending.map { GameRowItem(it) }, "trending", onOpen, eyebrow = "Last 90 days", onLongClick = onLongClick) }
        }
        item(key = "attribution") { RawgAttribution() }
    }
    // Once the hero scrolls away, cards mustn't slide under the clock.
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    if (scrolled) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Hero(picks: List<Pick>, onOpen: OpenGame, onSave: (Game) -> Unit) {
    val pager = rememberPagerState { picks.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(6_000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % picks.size)
        }
    }
    Column {
        HorizontalPager(pager, beyondViewportPageCount = 1) { page ->
            val pick = picks[page]
            val key = gameKey("hero", pick.game.id)
            Box(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(pick.game, key) })) {
                CoverImage(RawgImages.hero(pick.game.coverUrl), pick.game.name, aspectRatio = 0.8f, sharedKey = key, shape = RoundedCornerShape(0.dp), placeholder = Icons.Rounded.SportsEsports)
                Box(Modifier.matchParentSize().background(Brand.tokens.fadeToBackground))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        pick.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let {
                            MatchBadge(it, large = true)
                            Spacer(Modifier.width(8.dp))
                        }
                        ReasonPill(pick.reason.label())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(pick.game.name, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(
                            pick.game.releaseYear,
                            pick.game.genres.take(2).joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
                            pick.game.playtimeHours.takeIf { it > 0 }?.let { "$it h to beat" },
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientButton("More info", { onOpen(pick.game, key) }, Modifier.weight(1f), icon = Icons.Outlined.Info)
                        TonalPillButton("", { onSave(pick.game) }, icon = Icons.Rounded.Add, contentDescription = "Add to backlog")
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(picks.size) { i ->
                val selected = i == pager.currentPage
                Box(
                    Modifier.height(6.dp).width(if (selected) 22.dp else 6.dp).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}

/** All of the user's platforms, or just one: a pill row over the hero art. */
@Composable
private fun PlatformSwitch(platforms: List<Platform>, scope: Platform?, onScope: (Platform?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScopePill("All", scope == null) { onScope(null) } }
        items(platforms) { platform -> ScopePill(platform.label, scope == platform) { onScope(platform) } }
    }
}

@Composable
private fun ScopePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Color.White else Color.Black.copy(alpha = 0.42f),
        contentColor = if (selected) Color.Black else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

/** What the engine has learned, so the feed never feels like a black box. */
@Composable
private fun TasteCard(taste: TasteSummary) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("WHAT SHAPES YOUR PICKS", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${taste.liked} loved · ${taste.finished} finished",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            val all = (taste.tags + taste.genres)
            val max = all.maxOfOrNull { it.weight }?.coerceAtLeast(0.0001) ?: 1.0
            (taste.tags.take(3) + taste.genres.take(2)).forEach { WeightBar(it, max) }
        }
    }
}

@Composable
private fun WeightBar(item: Weighted, max: Double) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(item.name, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth((item.weight / max).toFloat().coerceIn(0.05f, 1f)).height(6.dp).clip(CircleShape).background(Brand.tokens.gradient))
        }
    }
}

private const val HERO_COUNT = 5
