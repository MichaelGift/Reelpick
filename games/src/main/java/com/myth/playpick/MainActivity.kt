package com.myth.playpick

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.myth.core.design.CoverImage
import com.myth.core.design.LoadingFeed
import com.myth.core.design.MessageState
import com.myth.core.design.SectionHeader
import com.myth.playpick.domain.model.Game
import com.myth.playpick.domain.model.GameQuery
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.ui.common.RawgAttribution
import com.myth.playpick.ui.common.RawgImages
import com.myth.playpick.ui.common.toUserMessage
import com.myth.playpick.ui.theme.PlaypickTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Temporary shell for the data layer phase: popular games straight from RAWG. Replaced by For You. */
@HiltViewModel
class ShellViewModel @Inject constructor(games: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow<Result<List<Game>>?>(null)
    val state: StateFlow<Result<List<Game>>?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = games.games(GameQuery(ordering = GameQuery.Ordering.POPULAR, pageSize = 20)).map { it.items }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            PlaypickTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val result = state
                    when {
                        result == null -> LoadingFeed(Modifier.statusBarsPadding())
                        result.isFailure -> MessageState(
                            icon = Icons.Rounded.SportsEsports,
                            title = "Couldn't load games",
                            message = result.exceptionOrNull()!!.toUserMessage(),
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.statusBarsPadding(),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Popular now", eyebrow = "Playpick") }
                            items(result.getOrThrow(), key = { it.id }) { game ->
                                Column {
                                    CoverImage(RawgImages.cover(game.coverUrl), game.name, aspectRatio = 16f / 9f)
                                    Spacer(Modifier.height(6.dp))
                                    Text(game.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        game.tags.take(3).joinToString(" · ") { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { RawgAttribution() }
                        }
                    }
                }
            }
        }
    }
}
