package com.myth.movieapp.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.Movie

/** Long-press menu for quick feedback without opening the details screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieActionsSheet(
    movie: Movie,
    onDismiss: () -> Unit,
    onFeedback: (FeedbackType) -> Unit,
    onAddToWatchlist: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                movie.releaseYear?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SheetAction(Icons.Outlined.ThumbUp, "Like — show me more like this") { onFeedback(FeedbackType.LIKE); onDismiss() }
            SheetAction(Icons.Outlined.ThumbDown, "Dislike") { onFeedback(FeedbackType.DISLIKE); onDismiss() }
            SheetAction(Icons.Outlined.Block, "Not interested") { onFeedback(FeedbackType.NOT_INTERESTED); onDismiss() }
            SheetAction(Icons.Outlined.BookmarkAdd, "Add to watchlist") { onAddToWatchlist(); onDismiss() }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
