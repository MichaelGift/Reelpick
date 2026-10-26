package com.myth.movieapp.ui.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.MessageState
import com.myth.movieapp.ui.common.OpenMovie
import com.myth.movieapp.ui.common.PosterCard
import com.myth.movieapp.ui.common.posterKey
import com.myth.movieapp.ui.theme.EyebrowStyle

@Composable
fun SearchScreen(onMovieClick: OpenMovie, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp))
        TextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            placeholder = { Text("Films, franchises, directors' work…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.onSubmit(); keyboard?.hide() }),
        )
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(CircleShape))

        when {
            state.isIdle -> Discover(
                recent = recent,
                genres = genres,
                onRecent = viewModel::onSubmit,
                onClearRecent = viewModel::clearRecentSearches,
                onGenre = { keyboard?.hide(); viewModel.browse(it) },
            )
            state.error != null && state.results.isEmpty() -> ErrorState(state.error!!, onRetry = {
                state.browsing?.let(viewModel::browse) ?: viewModel.onSubmit()
            })
            state.searched && state.results.isEmpty() && !state.isLoading -> MessageState(
                icon = Icons.Outlined.SearchOff,
                title = "No matches",
                message = "Nothing on TMDB matches “${state.query.trim()}”. Try a shorter title.",
            )
            else -> Results(state, onMovieClick, viewModel::loadMore, onClearBrowse = viewModel::clearBrowse)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Discover(
    recent: List<String>,
    genres: List<Genre>,
    onRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onGenre: (Genre) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("RECENT", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearRecent) { Text("Clear") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { query ->
                            Surface(onClick = { onRecent(query) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(query, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (genres.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "BROWSE BY GENRE",
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            items(genres, key = { it.id }) { genre -> GenreTile(genre, onClick = { onGenre(genre) }) }
        }
    }
}

private val genreTints = listOf(
    Color(0xFFB2453A), Color(0xFF3A63B2), Color(0xFF2F8A6E), Color(0xFF9A6A2E),
    Color(0xFF6A3AB2), Color(0xFFB23A7A), Color(0xFF2E7F96), Color(0xFF7A8A2E),
)

@Composable
private fun GenreTile(genre: Genre, onClick: () -> Unit) {
    val tint = genreTints[(genre.id % genreTints.size + genreTints.size) % genreTints.size]
    Box(
        Modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.35f))))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(genre.name, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun Results(state: SearchUiState, onMovieClick: OpenMovie, onLoadMore: () -> Unit, onClearBrowse: () -> Unit) {
    val gridState: LazyGridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.results.size) { if (nearEnd) onLoadMore() }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        state.browsing?.let { genre ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BROWSING", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
                        Text(genre.name, style = MaterialTheme.typography.headlineSmall)
                    }
                    IconButton(onClick = onClearBrowse) { Icon(Icons.Rounded.Close, contentDescription = "Stop browsing") }
                }
            }
        }
        items(state.results, key = { it.id }) { movie ->
            val key = posterKey("search", movie.id)
            PosterCard(
                movie = movie,
                width = 200.dp,
                sharedKey = key,
                onClick = { onMovieClick(movie, key) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
