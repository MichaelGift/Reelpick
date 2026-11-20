package com.myth.playpick.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.myth.playpick.domain.model.Achievement
import com.myth.playpick.domain.model.Rarity
import java.util.Locale

val Rarity.label: String
    get() = when (this) {
        Rarity.COMMON -> "Common"
        Rarity.UNCOMMON -> "Uncommon"
        Rarity.RARE -> "Rare"
        Rarity.ULTRA_RARE -> "Ultra rare"
    }

/** Rarity tiers read like loot colours, which players already know. */
val Rarity.color: Color
    get() = when (this) {
        Rarity.COMMON -> Color(0xFF9AA0A6)
        Rarity.UNCOMMON -> Color(0xFF5EE0A0)
        Rarity.RARE -> Color(0xFF60A5FA)
        Rarity.ULTRA_RARE -> Color(0xFFF4C46B)
    }

fun Achievement.percentLabel(): String? = percent?.let { String.format(Locale.US, if (it < 10) "%.1f%%" else "%.0f%%", it) }

@Composable
fun RarityBadge(achievement: Achievement, modifier: Modifier = Modifier) {
    val rarity = achievement.rarity ?: return
    Surface(modifier = modifier, shape = CircleShape, color = rarity.color.copy(alpha = 0.16f), contentColor = rarity.color) {
        Text(
            listOfNotNull(rarity.label, achievement.percentLabel()).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** The achievement's own icon, greyed out until it's unlocked. */
@Composable
fun AchievementIcon(achievement: Achievement, unlocked: Boolean, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        AsyncImage(
            model = RawgImages.icon(achievement.iconUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (unlocked) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            alpha = if (unlocked) 1f else 0.55f,
            modifier = Modifier.matchParentSize(),
        )
    }
}
