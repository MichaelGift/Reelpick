package com.myth.movieapp.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.Genre
import com.myth.movieapp.domain.model.Movie
import com.myth.movieapp.ui.common.ErrorState
import com.myth.movieapp.ui.common.GradientButton
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.common.PosterShape
import com.myth.movieapp.ui.common.Wordmark
import com.myth.movieapp.ui.onboarding.OnboardingUiState.Companion.MIN_GENRES
import com.myth.movieapp.ui.onboarding.OnboardingUiState.Companion.MIN_LIKES
import com.myth.movieapp.ui.onboarding.OnboardingUiState.Step
import com.myth.movieapp.ui.theme.Brand
import com.myth.movieapp.ui.theme.EyebrowStyle

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != Step.Welcome, onBack = viewModel::back)

    AnimatedContent(
        targetState = state.step,
        transitionSpec = { (slideInHorizontally { it / 6 } + fadeIn()) togetherWith fadeOut() },
        label = "onboardingStep",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { step ->
        when (step) {
            Step.Welcome -> Welcome(state.mosaic, onStart = viewModel::start)
            Step.Genres -> StepScaffold(
                stepLabel = "Step 1 of 2",
                title = "What do you love to watch?",
                subtitle = "Pick at least $MIN_GENRES genres. You can change your mind any time.",
                onBack = viewModel::back,
                cta = if (state.canContinue) "Continue" else "Pick ${MIN_GENRES - state.selectedGenreIds.size} more",
                ctaEnabled = state.canContinue,
                onCta = viewModel::continueToMovies,
            ) {
                when {
                    state.error != null -> ErrorState(state.error!!, viewModel::loadGenres)
                    state.isLoading && state.genres.isEmpty() -> Loading()
                    else -> GenreGrid(state.genres, state.selectedGenreIds, viewModel::toggleGenre)
                }
            }
            Step.Movies -> StepScaffold(
                stepLabel = "Step 2 of 2",
                title = "Films you've enjoyed",
                subtitle = "Tap at least $MIN_LIKES. Each one becomes a seed for your recommendations.",
                onBack = viewModel::back,
                cta = if (state.likedIds.size < MIN_LIKES) "Pick ${MIN_LIKES - state.likedIds.size} more" else "Build my feed",
                ctaEnabled = state.canFinish,
                onCta = viewModel::finish,
                progress = { SeedProgress(state.likedIds.size) },
            ) {
                when {
                    state.error != null -> ErrorState(state.error!!, viewModel::continueToMovies)
                    state.isLoading -> Loading()
                    else -> MoviePicker(state.movies, state.likedIds, viewModel::toggleLike)
                }
            }
        }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun Welcome(mosaic: List<Movie>, onStart: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PosterMosaic(mosaic, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                    0.45f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                    0.72f to MaterialTheme.colorScheme.background,
                ),
            ),
        )
        Wordmark(Modifier.statusBarsPadding().padding(24.dp))
        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("RECOMMENDATIONS YOU CAN SEE THROUGH", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text("Your next favourite film, and exactly why.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tell us what you love. Every pick comes with a taste match and the reason it's there.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            GradientButton("Get started", onStart, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

/** Three tilted columns of posters drifting in opposite directions behind the welcome copy. */
@Composable
private fun PosterMosaic(movies: List<Movie>, modifier: Modifier = Modifier) {
    if (movies.size < 6) return
    val drift by rememberInfiniteTransition(label = "mosaic").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val columns = movies.chunked((movies.size + 2) / 3).take(3)
    Row(
        modifier.graphicsLayer {
            rotationZ = -12f
            scaleX = 1.35f
            scaleY = 1.35f
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        columns.forEachIndexed { index, column ->
            val direction = if (index % 2 == 0) -1f else 1f
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer { translationY = direction * drift * 900f - (if (index == 1) 400f else 0f) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                (column + column).forEach { movie -> PosterImage(movie.posterPath, contentDescription = null) }
            }
        }
    }
}

@Composable
private fun StepScaffold(
    stepLabel: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    progress: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(stepLabel.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress != null) {
                Spacer(Modifier.height(16.dp))
                progress()
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f)) {
            content()
            // Fade content out under the floating CTA.
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(32.dp).background(
                    Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)),
                ),
            )
        }
        GradientButton(
            cta,
            onCta,
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = ctaEnabled,
        )
    }
}

/** Five segments that fill with the brand gradient as seeds are picked. */
@Composable
private fun SeedProgress(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(MIN_LIKES) { index ->
            val filled = index < count
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) Brand.tokens.gradient
                        else Brush.linearGradient(List(2) { MaterialTheme.colorScheme.surfaceContainerHighest }),
                    ),
            )
        }
    }
}

// Deep, desaturated tints so tiles read as a set rather than a rainbow.
private val tileTints = listOf(
    Color(0xFF7A2E3B), Color(0xFF2E4A7A), Color(0xFF2E6B5A), Color(0xFF6B4E2E),
    Color(0xFF4E2E7A), Color(0xFF7A5A2E), Color(0xFF2E6170), Color(0xFF5C2E5E),
)

@Composable
private fun GenreGrid(genres: List<Genre>, selected: Set<Int>, onToggle: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(genres, key = { it.id }) { genre ->
            val isSelected = genre.id in selected
            val tint = tileTints[(genre.id % tileTints.size + tileTints.size) % tileTints.size]
            val border by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "tileBorder",
            )
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier
                    .height(76.dp)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(tint.copy(alpha = if (isSelected) 0.95f else 0.45f), MaterialTheme.colorScheme.surfaceContainer),
                        ),
                    )
                    .border(if (isSelected) 2.dp else 1.dp, border, shape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(genre.id)
                    }
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    genre.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterStart).padding(end = 28.dp),
                )
                if (isSelected) {
                    Box(
                        Modifier.align(Alignment.CenterEnd).size(24.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MoviePicker(movies: List<Movie>, likedIds: Set<Int>, onToggle: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(movies, key = { it.id }) { movie ->
            val liked = movie.id in likedIds
            val scale by animateFloatAsState(if (liked) 0.94f else 1f, label = "pickScale")
            Box(
                Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(PosterShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(movie.id)
                    },
            ) {
                PosterImage(movie.posterPath, contentDescription = movie.title)
                if (liked) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(3.dp, Brand.tokens.gradient, PosterShape),
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).background(Brand.tokens.gradient, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Liked", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
