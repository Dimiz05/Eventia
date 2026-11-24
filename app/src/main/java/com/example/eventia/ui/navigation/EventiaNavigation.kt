package com.example.eventia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventia.R
import kotlinx.coroutines.flow.Flow
import com.example.eventia.ui.screens.AccountScreen
import com.example.eventia.ui.screens.EventDetailScreen
import com.example.eventia.ui.screens.ExploreScreen
import com.example.eventia.ui.screens.FavoriteNotice
import com.example.eventia.ui.screens.FavoritesScreen
import com.example.eventia.ui.screens.FavoritesViewModel
import com.example.eventia.ui.screens.MapScreen
import com.example.eventia.ui.screens.SessionUiState
import com.example.eventia.ui.screens.VenueDetailScreen

private enum class Tab(val route: String, val title: Int, val icon: Int) {
    Explore("explore", R.string.tab_explore, R.drawable.ic_explore),
    Map("map", R.string.tab_map, R.drawable.ic_map),
    Favorites("favorites", R.string.tab_favorites, R.drawable.ic_favorites),
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
    currentSession: SessionUiState.Active?,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    online: Flow<Boolean>,
    returnEventId: Long?,
    onEventReturnHandled: (Long) -> Unit,
    onEventSignIn: (Long) -> Unit,
    onEventSessionExpired: (Long) -> Unit,
    onAttendanceChanged: () -> Unit
) {
    val controller = rememberNavController()
    val session by rememberUpdatedState(currentSession)
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favorites by favoritesViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val connectionNotice by remember(online) { online.connectionNotices() }
        .collectAsStateWithLifecycle(initialValue = null)
    val removedMessage = stringResource(R.string.favorite_removed)
    val undoLabel = stringResource(R.string.favorite_undo)
    val mutationError = stringResource(R.string.favorite_change_error)
    val retryLabel = stringResource(R.string.retry)
    LaunchedEffect(favoritesViewModel, removedMessage, undoLabel, mutationError, retryLabel) {
        favoritesViewModel.notices.collect { notice ->
            when (notice) {
                is FavoriteNotice.Removed -> {
                    val result = snackbar.showSnackbar(removedMessage, undoLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) {
                        favoritesViewModel.changeFavorite(notice.event, save = true)
                    }
                }
                is FavoriteNotice.Failed -> {
                    val result = snackbar.showSnackbar(mutationError, retryLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) notice.retry()
                }
            }
        }
    }
    val entry by controller.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val selectedTab = Tab.entries.firstOrNull { it.route == route } ?: Tab.Explore
    val detail = DetailDestination.entries.firstOrNull { it.pattern == route }
    val eventDetail = detail == DetailDestination.Event
    val openEvent: (Long) -> Unit = { id ->
        controller.navigate(DetailDestination.Event.route(id)) { launchSingleTop = true }
    }
    val openVenue: (Long) -> Unit = { id ->
        controller.navigate(DetailDestination.Venue.route(id)) { launchSingleTop = true }
    }
    LaunchedEffect(returnEventId) {
        if (returnEventId != null) {
            val current = controller.currentBackStackEntry
            if (current?.destination?.route != DetailDestination.Event.pattern ||
                current.arguments?.getLong(DetailId) != returnEventId
            ) {
                openEvent(returnEventId)
            }
            onEventReturnHandled(returnEventId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = if (eventDetail) {
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!eventDetail) {
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
            }
        },
        bottomBar = {
            if (detail == null || connectionNotice != null) {
                Column(Modifier.navigationBarsPadding()) {
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
                    connectionNotice?.let { ConnectionBanner(it) }
                }
            }
        }
    ) { padding ->
        NavHost(controller, Tab.Explore.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Explore.route) {
                ExploreScreen(
                    onEventClick = openEvent,
                    onVenueClick = openVenue,
                    favorites = favorites,
                    onFavoriteChange = { event, save -> favoritesViewModel.changeFavorite(event, save) }
                )
            }
            composable(Tab.Map.route) { MapScreen(onEventClick = openEvent) }
            composable(Tab.Profile.route) {
                AccountScreen(session, onRetryAccount, onSignIn, onSignOut)
            }
            composable(Tab.Favorites.route) {
                FavoritesScreen(
                    state = favorites,
                    onEventClick = openEvent,
                    onRemove = { favoritesViewModel.changeFavorite(it, save = false, offerUndo = true) },
                    onRetry = favoritesViewModel::retry
                )
            }
            DetailDestination.entries.forEach { destination ->
                composable(
                    route = destination.pattern,
                    arguments = listOf(navArgument(DetailId) { type = NavType.LongType })
                ) { detailEntry ->
                    when (destination) {
                        DetailDestination.Event -> EventDetailScreen(
                            authenticated = session != null,
                            onEventSignIn = onEventSignIn,
                            onSessionExpired = { id ->
                                if (controller.currentBackStackEntry == detailEntry) onEventSessionExpired(id)
                            },
                            onBack = { controller.navigateUp() },
                            onExplore = { controller.popBackStack(Tab.Explore.route, inclusive = false) },
                            onAttendanceChanged = onAttendanceChanged,
                            onVenueClick = openVenue,
                            favorites = favorites,
                            onFavoriteChange = { event, save -> favoritesViewModel.changeFavorite(event, save) }
                        )
                        DetailDestination.Venue -> VenueDetailScreen(
                            onBack = { controller.navigateUp() },
                            onEventClick = openEvent
                        )
                    }
                }
            }
        }
    }
}
