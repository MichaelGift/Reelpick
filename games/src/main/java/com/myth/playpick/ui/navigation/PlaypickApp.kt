package com.myth.playpick.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myth.core.design.LocalNavAnimatedScope
import com.myth.core.design.LocalSharedTransitionScope
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.checklist.ChecklistScreen
import com.myth.playpick.ui.details.GameDetailsScreen
import com.myth.playpick.ui.library.LibraryScreen
import com.myth.playpick.ui.foryou.ForYouScreen
import com.myth.playpick.ui.search.SearchScreen
import com.myth.playpick.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object ForYouRoute
@Serializable data object SearchRoute
@Serializable data object LibraryRoute
@Serializable data object SettingsRoute

/** [sharedKey] is the tapped cover, so it can morph into the details header; name and art draw it before details load. */
@Serializable
data class GameRoute(
    val gameId: Int,
    val sharedKey: String? = null,
    val name: String = "",
    val coverUrl: String? = null,
)

@Serializable
data class ChecklistRoute(val gameId: Int, val name: String = "")

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(ForYouRoute, ForYouRoute::class, "For you", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    TopLevel(SearchRoute, SearchRoute::class, "Search", Icons.Rounded.Search, Icons.Outlined.Search),
    TopLevel(LibraryRoute, LibraryRoute::class, "Library", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    TopLevel(SettingsRoute, SettingsRoute::class, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
)

/** Room the floating bar takes at the bottom of a top-level screen; lists pad their end by this. */
val FloatingBarClearance = 104.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaypickApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination?.hasRoute(GameRoute::class) != true && destination?.hasRoute(ChecklistRoute::class) != true
    val openGame: OpenGame = { game, key -> navController.navigate(GameRoute(game.id, key, game.name, game.coverUrl)) }

    // The bar floats over the content (HUD style), so screens draw edge to edge and pad their lists instead.
    Box(Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController,
                    startDestination = ForYouRoute,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(180)) },
                ) {
                    composable<ForYouRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { ForYouScreen(onOpen = openGame) }
                    }
                    composable<SearchRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { SearchScreen(onOpen = openGame) }
                    }
                    composable<LibraryRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            LibraryScreen(onOpen = openGame, onChecklist = { id, name -> navController.navigate(ChecklistRoute(id, name)) })
                        }
                    }
                    composable<SettingsRoute> { SettingsScreen() }
                    composable<GameRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            GameDetailsScreen(
                                onBack = navController::popBackStack,
                                onOpen = openGame,
                                onChecklist = { id, name -> navController.navigate(ChecklistRoute(id, name)) },
                            )
                        }
                    }
                    composable<ChecklistRoute> { ChecklistScreen(onBack = navController::popBackStack) }
                }
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBar(
                selected = { item -> destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true },
                onSelect = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

/** A floating pill of four tabs; the current one is a filled accent tile. */
@Composable
private fun FloatingBar(selected: (TopLevel) -> Boolean, onSelect: (TopLevel) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        topLevel.forEach { item ->
            val isSelected = selected(item)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(item) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(if (isSelected) item.selectedIcon else item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Text(item.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = tint)
            }
        }
    }
}
