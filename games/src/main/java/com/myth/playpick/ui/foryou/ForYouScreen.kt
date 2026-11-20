package com.myth.playpick.ui.foryou

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.CoverImage
import com.myth.core.design.GradientButton
import com.myth.core.design.LoadingFeed
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.core.design.MessageState
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
import com.myth.playpick.ui.common.HudPill
import com.myth.playpick.ui.common.HudTag
import com.myth.playpick.ui.common.MatchTag
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.SegmentedBar
import com.myth.playpick.ui.common.Wordmark
import com.myth.playpick.ui.common.gameKey
import com.myth.playpick.ui.navigation.FloatingBarClearance
import com.myth.playpick.ui.theme.Hud
import com.myth.playpick.ui.theme.HudLabel
import com.myth.playpick.ui.theme.HudMeta
import kotlinx.coroutines.delay
import java.util.Locale

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
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(bottom = FloatingBarClearance),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "header") {
                Column(Modifier.statusBarsPadding().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Wordmark(Modifier.padding(horizontal = 16.dp))
                    if (state.platforms.size > 1) PlatformSwitch(state.platforms, state.scope, onScope)
                }
            }
            if (feed.isEmpty) {
                item { Text("Nothing matches on this platform yet. Try another, or pull to refresh.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (feed.topPicks.isNotEmpty()) {
                item(key = "hero") { HeroDeck(feed.topPicks.take(HERO_COUNT), onOpen, onSave) }
                item(key = "top") { GameRow("Top 10 for you", feed.topPicks.rowItems(), "top", onOpen, caption = "Ranked by taste", ranked = true, onLongClick = onLongClick) }
            }
            feed.taste?.takeIf { it.genres.isNotEmpty() || it.tags.isNotEmpty() }?.let { taste ->
                item(key = "taste") { TasteCard(taste) }
            }
            feed.becauseYouLiked.forEach { row ->
                item(key = "seed-${row.seed.id}") {
                    GameRow("Because you liked ${row.seed.name}", row.picks.rowItems(), "seed-${row.seed.id}", onOpen, onLongClick = onLongClick)
                }
            }
            if (feed.shortAndSweet.isNotEmpty()) {
                item(key = "short") { GameRow("Short & sweet", feed.shortAndSweet.rowItems(), "short", onOpen, caption = "Under 10 h", cardWidth = 160.dp, onLongClick = onLongClick) }
            }
            feed.genreRows.forEach { row ->
                item(key = "genre-${row.genre.id}") { GameRow(row.genre.name, row.picks.rowItems(), "genre-${row.genre.id}", onOpen, caption = "Your genres", onLongClick = onLongClick) }
            }
            if (feed.trending.isNotEmpty()) {
                item(key = "trending") { GameRow("Popular right now", feed.trending.map { GameRowItem(it) }, "trending", onOpen, caption = "Last 90 days", onLongClick = onLongClick) }
            }
            item(key = "attribution") { RawgAttribution() }
        }
        // Once the header scrolls away, cards mustn't slide under the clock.
        val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
        if (scrolled) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background))
    }
}

@Composable
private fun PlatformSwitch(platforms: List<Platform>, scope: Platform?, onScope: (Platform?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { HudPill("All", scope == null, { onScope(null) }) }
        items(platforms) { platform -> HudPill(platform.label, scope == platform, { onScope(platform) }) }
    }
}

/**
 * The top picks as a layered deck: the front card is full-bleed art (layer 1) with its details on
 * an inset panel (layer 2), and the next two cards peek out behind it. Swipe for the next pick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroDeck(picks: List<Pick>, onOpen: OpenGame, onSave: (Game) -> Unit) {
    val pager = rememberPagerState { picks.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(7_000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % picks.size)
        }
    }
    val shape = RoundedCornerShape(18.dp)
    val panelShape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(CARD_HEIGHT + 18.dp)) {
        // The deck's next cards, narrower and dimmer, behind the front one.
        listOf(24.dp to 0.5f, 12.dp to 0.8f).forEachIndexed { i, (inset, alpha) ->
            Box(
                Modifier
                    .padding(horizontal = inset)
                    .offset(y = (i * 9).dp)
                    .fillMaxWidth()
                    .height(CARD_HEIGHT - 12.dp)
                    .alpha(alpha)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            )
        }
        HorizontalPager(pager, modifier = Modifier.offset(y = 18.dp).height(CARD_HEIGHT), pageSpacing = 12.dp) { page ->
            val pick = picks[page]
            val key = gameKey("hero", pick.game.id)
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    .combinedClickable(onClick = { onOpen(pick.game, key) }),
            ) {
                CoverImage(
                    RawgImages.hero(pick.game.coverUrl), pick.game.name,
                    Modifier.fillMaxSize(), aspectRatio = 1f, sharedKey = key,
                    shape = shape, placeholder = Icons.Rounded.SportsEsports,
                )
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    pick.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let { MatchTag(it, long = true) } ?: Spacer(Modifier.width(1.dp))
                    HudTag(String.format(Locale.US, "%02d / %02d", page + 1, picks.size))
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .fillMaxWidth()
                        .clip(panelShape)
                        .background(Hud.Ink.copy(alpha = 0.88f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), panelShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pick.reason.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(pick.game.name, style = MaterialTheme.typography.headlineLarge, color = Hud.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            pick.game.releaseYear,
                            pick.game.genres.firstOrNull()?.name?.uppercase(),
                            pick.game.playtimeHours.takeIf { it > 0 }?.let { "$it H" },
                            pick.game.rating.takeIf { pick.game.ratingsCount > 0 }?.let { String.format(Locale.US, "★ %.1f", it) },
                        ).joinToString(" · "),
                        style = HudMeta,
                        color = Hud.Muted,
                        maxLines = 1,
                    )
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton("View game", { onOpen(pick.game, key) }, Modifier.weight(1f))
                        TonalPillButton("", { onSave(pick.game) }, icon = Icons.Rounded.Add, contentDescription = "Add to backlog")
                    }
                }
            }
        }
    }
}

/** What the engine has learned, as HUD bars: tags first (they say the most), then genres. */
@Composable
private fun TasteCard(taste: TasteSummary) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(Hud.CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, Hud.CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("What shapes your picks", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${taste.liked} LOVED · ${taste.finished} DONE", style = HudLabel, color = Hud.Label)
        }
        val shown = (taste.tags.take(3) + taste.genres.take(2)).distinctBy { it.name.lowercase() }
        val max = shown.maxOfOrNull { it.weight }?.coerceAtLeast(0.0001) ?: 1.0
        shown.forEach { WeightBar(it, max) }
    }
}

@Composable
private fun WeightBar(item: Weighted, max: Double) {
    val fraction = (item.weight / max).toFloat().coerceIn(0.05f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(item.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${(fraction * 100).toInt()}", style = HudMeta, color = Hud.Muted)
        }
        SegmentedBar(fraction, segments = 20, height = 6.dp)
    }
}

private const val HERO_COUNT = 5
private val CARD_HEIGHT = 380.dp
