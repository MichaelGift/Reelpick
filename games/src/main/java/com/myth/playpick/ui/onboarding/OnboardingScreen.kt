package com.myth.playpick.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.Brand
import com.myth.core.design.CoverImage
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.GradientButton
import com.myth.core.design.MessageState
import com.myth.core.design.PosterShape
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.Platform
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.Wordmark
import com.myth.playpick.ui.onboarding.OnboardingViewModel.Step

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != Step.Welcome) { viewModel.back() }
    AnimatedContent(targetState = state.step, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) }, label = "step") { step ->
        when (step) {
            Step.Welcome -> Welcome(state, onStart = viewModel::start)
            Step.Platforms -> Platforms(state, viewModel::togglePlatform, viewModel::back, viewModel::toPlatformsDone)
            Step.Taste -> Taste(state, viewModel::toggleGenre, viewModel::toggleTag, viewModel::back, viewModel::loadTaste, viewModel::toTasteDone)
            Step.Games -> Games(state, viewModel::toggleGame, viewModel::back, viewModel::loadGames, viewModel::finish)
        }
    }
}

@Composable
private fun Welcome(state: OnboardingUiState, onStart: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CoverMosaic(state)
        Box(Modifier.fillMaxSize().background(Brand.tokens.fadeToBackground))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Wordmark(markSize = 34.dp)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("RECOMMENDATIONS YOU CAN SEE THROUGH", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text("Your next favourite game, and every last achievement.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tell us what you play. Every pick shows a taste match and why it's there, and every game gets a completion checklist.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            GradientButton("Get started", onStart, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

/** Two slowly drifting, tilted columns of game art behind the welcome copy. */
@Composable
private fun CoverMosaic(state: OnboardingUiState) {
    val drift by rememberInfiniteTransition(label = "mosaic").animateFloat(
        initialValue = 0f,
        targetValue = -240f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val covers = state.mosaic.mapNotNull { it.coverUrl }
    Row(
        Modifier.fillMaxSize().rotate(-8f).graphicsLayer { scaleX = 1.35f; scaleY = 1.35f },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) { column ->
            Column(
                Modifier.weight(1f).graphicsLayer { translationY = if (column % 2 == 0) drift else -drift - 120f },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(6) { row ->
                    val url = covers.getOrNull((row * 3 + column) % covers.size.coerceAtLeast(1))
                    if (url == null) {
                        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.8f))
                    } else {
                        CoverImage(RawgImages.cover(url), null, aspectRatio = 0.8f, placeholder = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("STEP $step OF 3", style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f)) { content() }
        GradientButton(cta, onCta, Modifier.fillMaxWidth().padding(20.dp), enabled = ctaEnabled)
    }
}

private val Platform.icon: ImageVector
    get() = when (this) {
        Platform.PC -> Icons.Rounded.Computer
        Platform.PLAYSTATION -> Icons.Rounded.SportsEsports
        Platform.XBOX -> Icons.Rounded.VideogameAsset
        Platform.NINTENDO -> Icons.Rounded.Gamepad
        Platform.MOBILE -> Icons.Rounded.PhoneAndroid
        Platform.MAC -> Icons.Rounded.LaptopMac
    }

@Composable
private fun Platforms(state: OnboardingUiState, onToggle: (Platform) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    StepScaffold(
        step = 1,
        title = "Where do you play?",
        subtitle = "Every recommendation will be something you can actually play.",
        onBack = onBack,
        cta = if (state.platforms.isEmpty()) "Pick at least one" else "Continue",
        ctaEnabled = state.platforms.isNotEmpty(),
        onCta = onNext,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Platform.entries) { platform ->
                SelectTile(platform.label, platform in state.platforms, icon = platform.icon) { onToggle(platform) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Taste(
    state: OnboardingUiState,
    onGenre: (Int) -> Unit,
    onTag: (Int) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
) {
    val remaining = OnboardingViewModel.MIN_TASTE - state.tasteCount
    StepScaffold(
        step = 2,
        title = "What do you love to play?",
        subtitle = "Pick at least ${OnboardingViewModel.MIN_TASTE} genres or tags. Tags like Story Rich or Difficult say the most.",
        onBack = onBack,
        cta = if (remaining > 0) "Pick $remaining more" else "Continue",
        ctaEnabled = remaining <= 0,
        onCta = onNext,
    ) {
        when {
            state.isLoading -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(18.dp)) }
            }
            state.error != null && state.genres.isEmpty() -> MessageState(Icons.Outlined.CloudOff, "Couldn't load genres", state.error, actionLabel = "Try again", onAction = onRetry)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.genres, key = { "g${it.id}" }) { genre ->
                    SelectTile(genre.name, genre.id in state.selectedGenres) { onGenre(genre.id) }
                }
                if (state.tags.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text("TAGS", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.tags.forEach { tag -> TagChip(tag.name, tag.id in state.selectedTags) { onTag(tag.id) } }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Games(state: OnboardingUiState, onToggle: (Int) -> Unit, onBack: () -> Unit, onRetry: () -> Unit, onFinish: () -> Unit) {
    val remaining = OnboardingViewModel.MIN_GAMES - state.selectedGames.size
    StepScaffold(
        step = 3,
        title = "Games you loved",
        subtitle = "Pick at least ${OnboardingViewModel.MIN_GAMES}. They seed your first recommendations.",
        onBack = onBack,
        cta = when {
            state.isFinishing -> "Building your feed…"
            remaining > 0 -> "Pick $remaining more"
            else -> "Build my feed"
        },
        ctaEnabled = remaining <= 0 && !state.isFinishing,
        onCta = onFinish,
    ) {
        when {
            state.isLoading -> LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(8) { ShimmerBox(Modifier.fillMaxWidth().aspectRatio(COVER_RATIO)) }
            }
            state.games.isEmpty() -> MessageState(
                Icons.Outlined.CloudOff,
                "No games found",
                state.error ?: "Try going back and picking a few more genres or tags.",
                actionLabel = "Try again",
                onAction = onRetry,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.games, key = { it.id }) { game ->
                    val selected = game.id in state.selectedGames
                    val haptics = LocalHapticFeedback.current
                    Column(
                        Modifier.clip(PosterShape).clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle(game.id)
                        },
                    ) {
                        Box {
                            CoverImage(RawgImages.cover(game.coverUrl), game.name, aspectRatio = COVER_RATIO, placeholder = Icons.Rounded.SportsEsports)
                            if (selected) {
                                Box(
                                    Modifier.matchParentSize().clip(PosterShape).border(3.dp, MaterialTheme.colorScheme.primary, PosterShape)
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                )
                                Check(Modifier.align(Alignment.TopEnd).padding(8.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(game.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { RawgAttribution() }
            }
        }
    }
}

@Composable
private fun SelectTile(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (icon != null) 96.dp else 64.dp)
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(Brand.tokens.gradientColors.map { it.copy(alpha = 0.32f) })
                else Brush.linearGradient(List(2) { MaterialTheme.colorScheme.surfaceContainerHigh }),
            )
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Check(Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

@Composable
private fun Check(modifier: Modifier = Modifier) {
    Box(modifier.size(26.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}
