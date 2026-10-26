package com.myth.movieapp.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.ui.common.MessageState
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.common.RatingBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(onMovieClick: (Int) -> Unit, viewModel: WatchlistViewModel = hiltViewModel()) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun withUndo(movie: Movie, message: String, action: (Movie) -> Unit, undo: (Movie) -> Unit) {
        action(movie)
        scope.launch {
            if (snackbar.showSnackbar(message, actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                undo(movie)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Watchlist") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = movies ?: return@Scaffold
        if (list.isEmpty()) {
            MessageState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Nothing saved yet",
                message = "Save movies from their details page. Your watchlist works offline.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(list, key = { it.id }) { movie ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.EndToStart ->
                                withUndo(movie, "Removed ${movie.title}", viewModel::remove, viewModel::restore)
                            SwipeToDismissBoxValue.StartToEnd ->
                                withUndo(movie, "Marked ${movie.title} as watched", viewModel::markWatched, viewModel::undoWatched)
                            SwipeToDismissBoxValue.Settled -> Unit
                        }
                        value != SwipeToDismissBoxValue.Settled
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
                ) {
                    WatchlistItem(movie, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (icon, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(Icons.Rounded.CheckCircle, "Watched", Alignment.CenterStart)
        else -> Triple(Icons.Rounded.Delete, "Remove", Alignment.CenterEnd)
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun WatchlistItem(movie: Movie, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(movie.posterPath, contentDescription = movie.title, modifier = Modifier.width(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(movie.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            movie.releaseYear?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (movie.voteCount > 0) RatingBadge(movie.voteAverage)
    }
}
