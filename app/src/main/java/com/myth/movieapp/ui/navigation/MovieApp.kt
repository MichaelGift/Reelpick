package com.myth.movieapp.ui.navigation

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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import com.myth.movieapp.ui.common.LocalNavAnimatedScope
import com.myth.movieapp.ui.common.LocalSharedTransitionScope
import com.myth.movieapp.ui.common.OpenMovie
import com.myth.movieapp.ui.details.DetailsScreen
import com.myth.movieapp.ui.foryou.ForYouScreen
import com.myth.movieapp.ui.search.SearchScreen
import com.myth.movieapp.ui.settings.SettingsScreen
import com.myth.movieapp.ui.watchlist.WatchlistScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object ForYouRoute
@Serializable data object SearchRoute
@Serializable data object WatchlistRoute
@Serializable data object SettingsRoute

/**
 * [sharedKey] identifies the poster that was tapped, so it can morph into the details poster;
 * title and artwork paths let details draw its header before the full record loads.
 */
@Serializable
data class DetailsRoute(
    val movieId: Int,
    val sharedKey: String? = null,
    val title: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
)

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(ForYouRoute, ForYouRoute::class, "For you", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    TopLevel(SearchRoute, SearchRoute::class, "Search", Icons.Rounded.Search, Icons.Outlined.Search),
    TopLevel(WatchlistRoute, WatchlistRoute::class, "Watchlist", Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder),
    TopLevel(SettingsRoute, SettingsRoute::class, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
)

/** Main app shell shown after onboarding: bottom navigation plus a full-screen details destination. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination?.hasRoute(DetailsRoute::class) != true

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
        val openDetails: OpenMovie = { movie, key ->
            navController.navigate(DetailsRoute(movie.id, key, movie.title, movie.posterPath, movie.backdropPath))
        }
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
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { ForYouScreen(onMovieClick = openDetails) }
                    }
                    composable<SearchRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { SearchScreen(onMovieClick = openDetails) }
                    }
                    composable<WatchlistRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) { WatchlistScreen(onMovieClick = openDetails) }
                    }
                    composable<SettingsRoute> { SettingsScreen() }
                    composable<DetailsRoute> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            DetailsScreen(onBack = navController::popBackStack, onMovieClick = openDetails)
                        }
                    }
                }
            }
        }
    }
}
