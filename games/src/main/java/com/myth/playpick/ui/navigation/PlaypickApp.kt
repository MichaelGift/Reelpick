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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.myth.core.design.MessageState
import com.myth.playpick.ui.common.OpenGame
import com.myth.playpick.ui.foryou.ForYouScreen
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

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(ForYouRoute, ForYouRoute::class, "For you", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    TopLevel(SearchRoute, SearchRoute::class, "Search", Icons.Rounded.Search, Icons.Outlined.Search),
    TopLevel(LibraryRoute, LibraryRoute::class, "Library", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    TopLevel(SettingsRoute, SettingsRoute::class, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaypickApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination?.hasRoute(GameRoute::class) != true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(300)) { it } + fadeIn(),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                        topLevel.forEach { item ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val openGame: OpenGame = { game, key -> navController.navigate(GameRoute(game.id, key, game.name, game.coverUrl)) }
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController,
                    startDestination = ForYouRoute,
                    modifier = Modifier.fillMaxWidth().padding(padding).consumeWindowInsets(padding),
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(180)) },
                ) {
                    composable<ForYouRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { ForYouScreen(onOpen = openGame) }
                    }
                    composable<SearchRoute> { Placeholder("Search") }
                    composable<LibraryRoute> { Placeholder("Library") }
                    composable<SettingsRoute> { Placeholder("Settings") }
                    composable<GameRoute> { Placeholder("Game details") }
                }
            }
        }
    }
}

/** Screens still to come on this branch. */
@Composable
private fun Placeholder(name: String) {
    MessageState(Icons.Outlined.Construction, name, "Coming in the next commit.")
}
