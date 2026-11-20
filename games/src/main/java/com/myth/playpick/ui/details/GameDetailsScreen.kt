package com.myth.playpick.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.core.design.Brand
import com.myth.core.design.Card
import com.myth.core.design.CoverImage
import com.myth.core.design.EyebrowStyle
import com.myth.core.design.GlassIconButton
import com.myth.core.design.GradientButton
import com.myth.core.design.MessageState
import com.myth.core.design.RatingPill
import com.myth.core.design.SectionHeader
import com.myth.core.design.ShimmerBox
import com.myth.playpick.domain.model.FeedbackType
import com.myth.playpick.domain.model.LibraryEntry
import com.myth.playpick.domain.model.LibraryStatus
import com.myth.playpick.ui.common.AchievementIcon
import com.myth.playpick.ui.common.COVER_RATIO
import com.myth.playpick.ui.common.GameRow
import com.myth.playpick.ui.common.GameRowItem
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.common.RarityBadge
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages

@Composable
fun GameDetailsScreen(
    onBack: () -> Unit,
    onOpen: OpenGame,
    onChecklist: (gameId: Int, name: String) -> Unit,
    viewModel: GameDetailsViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val entry by viewModel.libraryEntry.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val loaded = content as? DetailsContent.Loaded
    val name = loaded?.details?.game?.name ?: viewModel.preview.name
    // Derived, so the screen recomposes when the threshold is crossed, not on every scrolled pixel.
    val pastHeader by remember { derivedStateOf { scroll.value > 700 } }
    val scrim by animateFloatAsState(if (pastHeader) 1f else 0f, label = "scrim")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // The header draws from the route at once, so the shared cover morph has a target.
            Box {
                CoverImage(
                    RawgImages.hero(loaded?.details?.game?.coverUrl ?: viewModel.preview.coverUrl),
                    name,
                    aspectRatio = 0.9f,
                    sharedKey = viewModel.sharedKey,
                    shape = RoundedCornerShape(0.dp),
                    placeholder = Icons.Rounded.SportsEsports,
                )
                Box(Modifier.matchParentSize().background(Brand.tokens.fadeToBackground))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp)) {
                    Text(name, style = MaterialTheme.typography.displaySmall)
                    loaded?.let { HeaderMeta(it) }
                }
            }
            when (val c = content) {
                DetailsContent.Loading -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(if (it == 0) 54.dp else 18.dp), RoundedCornerShape(12.dp)) }
                }
                is DetailsContent.Error -> MessageState(Icons.Outlined.CloudOff, "Couldn't load this game", c.message, Modifier.height(420.dp), "Try again", viewModel::load)
                is DetailsContent.Loaded -> Body(c, rating, entry, onOpen, viewModel::rate, viewModel::setStatus) { onChecklist(viewModel.gameId, name) }
            }
        }
        // Back and the title stay readable once the art scrolls away.
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = scrim)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp).alpha(scrim),
            )
        }
    }
}

@Composable
private fun HeaderMeta(c: DetailsContent.Loaded) {
    val game = c.details.game
    Spacer(Modifier.height(8.dp))
    Text(
        listOfNotNull(
            game.releaseYear ?: "TBA".takeIf { game.tba },
            game.playtimeHours.takeIf { it > 0 }?.let { "$it h to beat" },
            game.esrb,
        ).joinToString("  ·  "),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (game.ratingsCount > 0) RatingPill(game.rating, votes = game.ratingsCount)
        game.metacritic?.let {
            Spacer(Modifier.width(14.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = metacriticColor(it)) {
                Text("$it", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text("Metacritic", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun metacriticColor(score: Int) = when {
    score >= 75 -> androidx.compose.ui.graphics.Color(0xFF66CC33)
    score >= 50 -> androidx.compose.ui.graphics.Color(0xFFFFCC33)
    else -> androidx.compose.ui.graphics.Color(0xFFFF6874)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Body(
    c: DetailsContent.Loaded,
    rating: FeedbackType?,
    entry: LibraryEntry?,
    onOpen: OpenGame,
    onRate: (FeedbackType) -> Unit,
    onStatus: (LibraryStatus?) -> Unit,
    onChecklist: () -> Unit,
) {
    val details = c.details
    val game = details.game
    val uri = LocalUriHandler.current
    Column(Modifier.navigationBarsPadding().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.platforms.forEach { Chip(it.name, outlined = false) }
                // RAWG has "RPG" as both a genre and a tag; show each label once.
                (game.genres + game.tags.take(5)).distinctBy { it.name.lowercase() }.forEach { Chip(it.name) }
            }
            c.fit?.let { FitCard(it) }
            LibraryControls(entry, onStatus)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Toggle(if (rating == FeedbackType.LIKE) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp, "Loved it", rating == FeedbackType.LIKE, Modifier.weight(1f)) { onRate(FeedbackType.LIKE) }
                Toggle(Icons.Outlined.Block, "Not for me", rating == FeedbackType.NOT_INTERESTED, Modifier.weight(1f)) { onRate(FeedbackType.NOT_INTERESTED) }
            }
        }

        if (details.achievementCount > 0) AchievementsCard(c, entry, onChecklist)

        if (details.description.isNotBlank()) Description(details.description)

        if (details.screenshots.isNotEmpty()) {
            Column {
                SectionHeader("Screenshots")
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(details.screenshots) { shot -> CoverImage(RawgImages.cover(shot), null, Modifier.width(260.dp), aspectRatio = COVER_RATIO, placeholder = null) }
                }
            }
        }

        val credits = listOfNotNull(
            details.developers.takeIf { it.isNotEmpty() }?.let { "Developed by " + it.joinToString { d -> d.name } },
            details.publishers.filter { p -> details.developers.none { it.id == p.id } }.takeIf { it.isNotEmpty() }?.let { "Published by " + it.joinToString { p -> p.name } },
        )
        if (credits.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                credits.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        if (details.stores.isNotEmpty() || details.website != null) {
            Column {
                SectionHeader("Where to get it")
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(details.stores) { link -> LinkChip(link.store.name) { uri.openUri(link.url) } }
                    details.website?.let { site -> item { LinkChip("Official site") { uri.openUri(site) } } }
                }
            }
        }

        if (c.series.isNotEmpty()) GameRow("In the series", c.series.map { GameRowItem(it) }, "series", onOpen)
        if (c.additions.isNotEmpty()) GameRow("DLC & editions", c.additions.map { GameRowItem(it) }, "dlc", onOpen)
        if (c.similar.isNotEmpty()) {
            GameRow("More like this", c.similar.map { (g, m) -> GameRowItem(g, match = m) }, "similar", onOpen, eyebrow = "Shares its tags")
        }
        RawgAttribution(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun FitCard(fit: TasteFit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(Brand.tokens.gradient, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${fit.match}%", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("TASTE MATCH", style = EyebrowStyle, color = Brand.tokens.match)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (fit.reasons.isEmpty()) "Close to what you play" else "You tend to love " + fit.reasons.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Status pills; tapping the active one takes the game out of the library. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryControls(entry: LibraryEntry?, onStatus: (LibraryStatus?) -> Unit) {
    Column {
        Text(if (entry == null) "ADD TO YOUR LIBRARY" else "IN YOUR LIBRARY", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryStatus.entries.forEach { status ->
                val selected = entry?.status == status
                Surface(
                    onClick = { onStatus(if (selected) null else status) },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(status.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(c: DetailsContent.Loaded, entry: LibraryEntry?, onChecklist: () -> Unit) {
    val count = c.details.achievementCount
    val progress = entry?.progress?.takeIf { it.total > 0 }
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Brand.tokens.rating)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("$count achievements", style = MaterialTheme.typography.titleMedium)
                    Text(
                        progress?.let { "${it.unlocked} of ${it.total} unlocked" } ?: "Track them as a completion checklist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (c.rarest.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("THE HARDEST", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                c.rarest.forEach { a ->
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AchievementIcon(a, unlocked = true, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        RarityBadge(a)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GradientButton(if (progress != null) "Open checklist" else "Track completion", onChecklist, Modifier.fillMaxWidth(), icon = Icons.Rounded.Checklist)
        }
    }
}

@Composable
private fun Description(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("ABOUT", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Show less" else "Read more", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Chip(label: String, outlined: Boolean = true) {
    Surface(
        shape = CircleShape,
        color = if (outlined) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (outlined) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun LinkChip(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Toggle(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
