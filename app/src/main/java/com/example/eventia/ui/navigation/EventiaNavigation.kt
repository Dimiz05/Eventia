package com.example.eventia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventia.R
import com.example.eventia.ui.screens.AccountScreen
import com.example.eventia.ui.screens.EventDetailScreen
import com.example.eventia.ui.screens.ExploreScreen
import com.example.eventia.ui.screens.MapScreen
import com.example.eventia.ui.screens.SessionUiState

private enum class Tab(val route: String, val title: Int, val icon: Int, val message: Int? = null) {
    Explore("explore", R.string.tab_explore, R.drawable.ic_explore),
    Map("map", R.string.tab_map, R.drawable.ic_map),
    Favorites("favorites", R.string.tab_favorites, R.drawable.ic_favorites, R.string.favorites_pending),
    Profile("profile", R.string.tab_profile, R.drawable.ic_profile)
}

internal const val DetailId = "id"

enum class DetailDestination(private val path: String, val title: Int) {
    Event("events", R.string.detail_event),
    Venue("venues", R.string.detail_venue);

    val pattern: String get() = "$path/{$DetailId}"

    fun route(id: Long): String = "$path/$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventiaNavigation(
    session: SessionUiState.Active?,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val controller = rememberNavController()
    val currentSession by rememberUpdatedState(session)
    val entry by controller.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val selectedTab = Tab.entries.firstOrNull { it.route == route } ?: Tab.Explore
    val detail = DetailDestination.entries.firstOrNull { it.pattern == route }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(detail?.title ?: selectedTab.title)) },
                navigationIcon = {
                    if (detail != null) {
                        IconButton(onClick = { controller.navigateUp() }) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (detail == null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = {
                                if (tab != selectedTab) {
                                    controller.navigate(tab.route) {
                                        popUpTo(controller.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.title)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(controller, Tab.Explore.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Explore.route) {
                ExploreScreen(onEventClick = { id ->
                    controller.navigate(DetailDestination.Event.route(id)) {
                        launchSingleTop = true
                    }
                })
            }
            composable(Tab.Map.route) { MapScreen() }
            composable(Tab.Profile.route) {
                AccountScreen(currentSession, onRetryAccount, onSignIn, onSignOut)
            }
            Tab.entries.filter { it.message != null }.forEach { tab ->
                composable(tab.route) {
                    Text(stringResource(requireNotNull(tab.message)), Modifier.padding(24.dp))
                }
            }
            DetailDestination.entries.forEach { destination ->
                composable(
                    route = destination.pattern,
                    arguments = listOf(navArgument(DetailId) { type = NavType.LongType })
                ) { detailEntry ->
                    when (destination) {
                        DetailDestination.Event -> EventDetailScreen(
                            onBack = { controller.navigateUp() },
                            onVenueClick = { id ->
                                controller.navigate(DetailDestination.Venue.route(id)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        DetailDestination.Venue -> {
                            val id = requireNotNull(detailEntry.arguments).getLong(DetailId)
                            Text(stringResource(R.string.detail_pending, id), Modifier.padding(24.dp))
                        }
                    }
                }
            }
        }
    }
}
