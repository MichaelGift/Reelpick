package com.myth.movieapp.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.ui.common.MessageState
import com.myth.movieapp.ui.common.OpenMovie
import com.myth.movieapp.ui.common.PosterCard
import com.myth.movieapp.ui.common.posterKey
import com.myth.movieapp.ui.theme.EyebrowStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(onMovieClick: OpenMovie, viewModel: WatchlistViewModel = hiltViewModel()) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var actionsFor by remember { mutableStateOf<Movie?>(null) }

    fun withUndo(movie: Movie, message: String, action: (Movie) -> Unit, undo: (Movie) -> Unit) {
        action(movie)
        scope.launch {
            if (snackbar.showSnackbar(message, actionLabel = "Undo") == SnackbarResult.ActionPerformed) undo(movie)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val list = movies
        when {
            list == null -> Unit
            list.isEmpty() -> MessageState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Your watchlist is empty",
                message = "Tap + on any film to save it for later. Your watchlist works offline.",
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                modifier = Modifier.statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text("Watchlist", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "${list.size} ${if (list.size == 1) "film" else "films"} saved  ·  long-press for options".uppercase(),
                            style = EyebrowStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list, key = { it.id }) { movie ->
                    val key = posterKey("watchlist", movie.id)
                    PosterCard(
                        movie = movie,
                        width = 200.dp,
                        sharedKey = key,
                        onClick = { onMovieClick(movie, key) },
                        onLongClick = { actionsFor = movie },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    actionsFor?.let { movie ->
        ModalBottomSheet(onDismissRequest = { actionsFor = null }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(movie.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                SheetAction(Icons.Outlined.CheckCircle, "Mark as watched") {
                    actionsFor = null
                    withUndo(movie, "Marked “${movie.title}” as watched", viewModel::markWatched, viewModel::undoWatched)
                }
                SheetAction(Icons.Outlined.Delete, "Remove from watchlist") {
                    actionsFor = null
                    withUndo(movie, "Removed “${movie.title}”", viewModel::remove, viewModel::restore)
                }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.titleMedium) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
