package com.myth.playpick.ui.search

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.playpick.ui.theme.HudLabel
import com.myth.core.design.MessageState
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.Named
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.GameCard
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.gameKey
import com.myth.playpick.ui.navigation.FloatingBarClearance
import com.myth.playpick.ui.theme.Hud

@Composable
fun SearchScreen(onOpen: OpenGame, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 14.dp))
        TextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Games, series, studios…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty() || state.browsing != null) {
                    IconButton(onClick = viewModel::clear) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
                }
            },
            singleLine = true,
            shape = Hud.ControlShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submit(); focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        val showingResults = state.searched || state.isLoading || state.browsing != null
        when {
            !showingResults -> Home(state, viewModel::useRecent, viewModel::clearRecent, viewModel::browse)
            state.isLoading && state.results.isEmpty() -> LoadingGrid()
            state.error != null && state.results.isEmpty() -> MessageState(Icons.Outlined.CloudOff, "Couldn't search", state.error!!, actionLabel = "Try again", onAction = viewModel::submit)
            state.results.isEmpty() -> MessageState(Icons.Outlined.SearchOff, "No matches", "Nothing on RAWG matches “${state.query}”.")
            else -> Results(state, onOpen, viewModel::loadMore)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Home(state: SearchUiState, onRecent: (String) -> Unit, onClearRecent: () -> Unit, onBrowse: (Named) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = FloatingBarClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("RECENT", style = HudLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearRecent) { Text("Clear") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recent.forEach { q ->
                            Surface(onClick = { onRecent(q) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                    Text(q, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        if (state.genres.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("BROWSE BY GENRE", style = HudLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.genres, key = { it.id }) { genre -> GenreTile(genre) { onBrowse(genre) } }
        }
    }
}

private val tints = listOf(0xFF6D3BE6, 0xFF0E7490, 0xFFB4235A, 0xFF8A5A00, 0xFF1F7A4D, 0xFF3B4BC8)

@Composable
private fun GenreTile(genre: Named, onClick: () -> Unit) {
    val tint = Color(tints[genre.id.mod(tints.size)])
    Box(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceContainerHigh)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(genre.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun LoadingGrid() {
    LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(8) { ShimmerBox(Modifier.fillMaxWidth().aspectRatio(COVER_RATIO)) }
    }
}

@Composable
private fun Results(state: SearchUiState, onOpen: OpenGame, onLoadMore: () -> Unit) {
    val grid = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= grid.layoutInfo.totalItemsCount - 6 } }
    LaunchedEffect(nearEnd, state.results.size) { if (nearEnd) onLoadMore() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = grid,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = FloatingBarClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        state.browsing?.let { genre ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("POPULAR IN ${genre.name.uppercase()} ON YOUR PLATFORMS", style = HudLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
        items(state.results, key = { it.id }) { game ->
            val key = gameKey("search", game.id)
            GameCard(game, onClick = { onOpen(game, key) }, width = null, sharedKey = key)
        }
        if (state.isLoading) {
            items(2) { ShimmerBox(Modifier.fillMaxWidth().aspectRatio(COVER_RATIO)) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { RawgAttribution() }
    }
}
