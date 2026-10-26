package com.myth.movieapp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
@Serializable data class DetailsRoute(val movieId: Int)

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
@Composable
fun MovieApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination?.hasRoute(DetailsRoute::class) != true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val openDetails: (Int) -> Unit = { navController.navigate(DetailsRoute(it)) }
        NavHost(
            navController = navController,
            startDestination = ForYouRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<ForYouRoute> { ForYouScreen(onMovieClick = openDetails) }
            composable<SearchRoute> { SearchScreen(onMovieClick = openDetails) }
            composable<WatchlistRoute> { WatchlistScreen(onMovieClick = openDetails) }
            composable<SettingsRoute> { SettingsScreen() }
            composable<DetailsRoute> {
                DetailsScreen(onBack = navController::popBackStack, onMovieClick = openDetails)
            }
        }
    }
}
