package com.myth.movieapp.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.onboarding.OnboardingUiState.Companion.MIN_GENRES
import com.myth.movieapp.ui.onboarding.OnboardingUiState.Companion.MIN_LIKES

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onMovies = state.step == OnboardingUiState.Step.Movies
    BackHandler(enabled = onMovies, onBack = viewModel::backToGenres)

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (onMovies) {
                        Button(onClick = viewModel::finish, enabled = state.canFinish, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (state.likedIds.size < MIN_LIKES) "Pick ${MIN_LIKES - state.likedIds.size} more"
                                else "Build my feed",
                            )
                        }
                    } else {
                        Button(onClick = viewModel::continueToMovies, enabled = state.canContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (state.selectedGenreIds.size < MIN_GENRES) "Pick ${MIN_GENRES - state.selectedGenreIds.size} more"
                                else "Continue",
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Header(
                title = if (onMovies) "Movies you've enjoyed" else "What do you like to watch?",
                subtitle = if (onMovies) "Tap at least $MIN_LIKES. These become the seeds for your recommendations."
                else "Choose at least $MIN_GENRES genres.",
                onBack = if (onMovies) viewModel::backToGenres else null,
            )
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = if (onMovies) viewModel::continueToMovies else viewModel::loadGenres,
                )
                onMovies -> MoviePicker(state.movies, state.likedIds, viewModel::toggleLike)
                else -> GenrePicker(state, viewModel::toggleGenre)
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: (() -> Unit)?) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("← Genres") }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenrePicker(state: OnboardingUiState, onToggle: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.genres.forEach { genre ->
            val selected = genre.id in state.selectedGenreIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(genre.id) },
                label = { Text(genre.name) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
            )
        }
    }
}

@Composable
private fun MoviePicker(movies: List<Movie>, likedIds: Set<Int>, onToggle: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(movies, key = { it.id }) { movie ->
            val liked = movie.id in likedIds
            Box(Modifier.clickable { onToggle(movie.id) }) {
                PosterImage(
                    movie.posterPath,
                    contentDescription = movie.title,
                    modifier = Modifier.alpha(if (liked || likedIds.isEmpty()) 1f else 0.75f),
                )
                if (liked) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Liked",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp).size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
