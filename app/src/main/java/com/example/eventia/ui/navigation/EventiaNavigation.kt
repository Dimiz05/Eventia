package com.example.eventia.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.DisposableEffect

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.screens.eventdetail.EventDetailScreen
import com.example.eventia.ui.screens.explore.ExploreScreen
import com.example.eventia.ui.components.ProfileAvatar
import com.example.eventia.ui.screens.favorites.FavoriteNotice
import com.example.eventia.ui.screens.favorites.FavoritesScreen
import com.example.eventia.ui.screens.favorites.FavoritesViewModel
import com.example.eventia.ui.screens.map.MapScreen
import com.example.eventia.ui.screens.profile.ProfileRoute
import com.example.eventia.ui.screens.session.SessionUiState
import com.example.eventia.ui.screens.venue.VenueDetailScreen
import kotlinx.coroutines.flow.Flow

private enum class Tab(val route: String, val title: Int, val icon: Int, val filledIcon: Int) {
    Explore("explore", R.string.tab_explore, R.drawable.ic_explore, R.drawable.ic_explore_filled),
    Map("map", R.string.tab_map, R.drawable.ic_map, R.drawable.ic_map_filled),
    Favorites("favorites", R.string.tab_favorites, R.drawable.ic_favorite_outline, R.drawable.ic_favorites),
    Profile("profile", R.string.tab_profile, R.drawable.ic_profile_outline, R.drawable.ic_profile)
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
    onProfileSessionExpired: () -> Unit,
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
    val savedMessage = stringResource(R.string.favorite_saved)
    val removedMessage = stringResource(R.string.favorite_removed)
    val undoLabel = stringResource(R.string.favorite_undo)
    val mutationError = stringResource(R.string.favorite_change_error)
    val retryLabel = stringResource(R.string.retry)
    LaunchedEffect(favoritesViewModel, savedMessage, removedMessage, undoLabel, mutationError, retryLabel) {
        favoritesViewModel.notices.collect { notice ->
            when (notice) {
                FavoriteNotice.Saved -> snackbar.showSnackbar(savedMessage)
                is FavoriteNotice.Removed -> {
                    val result = snackbar.showSnackbar(
                        removedMessage,
                        actionLabel = undoLabel.takeIf { notice.offerUndo },
                        duration = if (notice.offerUndo) SnackbarDuration.Long else SnackbarDuration.Short
                    )
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
    val isProfile = route == Tab.Profile.route
    val window = LocalActivity.current?.window
    DisposableEffect(window, isProfile) {
        if (window != null && isProfile) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        } else {
            onDispose { }
        }
    }
    val openTab: (Tab) -> Unit = { tab ->
        if (route != tab.route) {
            controller.navigate(tab.route) {
                popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val avatar: @Composable () -> Unit = {
        ProfileAvatar(signedIn = session != null) { openTab(Tab.Profile) }
    }
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
        contentWindowInsets = if (detail != null || isProfile) {
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (detail == null || connectionNotice != null) {
                Column(Modifier.navigationBarsPadding()) {
                    if (detail == null) FloatingTabBar(selectedTab, openTab)
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
                    avatar = avatar,
                    favorites = favorites,
                    onFavoriteChange = { event, save -> favoritesViewModel.changeFavorite(event, save, offerUndo = !save) }
                )
            }
            composable(Tab.Map.route) {
                MapScreen(onEventClick = openEvent, avatar = avatar, favoriteAction = { event ->
                    val selected = event.id in favorites.ids
                    FavoriteButton(selected, favorites.canChange(event.id)) {
                        favoritesViewModel.changeFavorite(event, !selected, offerUndo = selected)
                    }
                })
            }
            composable(Tab.Profile.route) {
                ProfileRoute(session, onRetryAccount, onSignIn, onSignOut, openEvent, onProfileSessionExpired)
            }
            composable(Tab.Favorites.route) {
                FavoritesScreen(
                    state = favorites,
                    onEventClick = openEvent,
                    onRemove = { favoritesViewModel.changeFavorite(it, save = false, offerUndo = true) },
                    onRetry = favoritesViewModel::retry,
                    avatar = avatar,
                    onExplore = { openTab(Tab.Explore) }
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
                            onFavoriteChange = { event, save -> favoritesViewModel.changeFavorite(event, save, offerUndo = !save) }
                        )
                        DetailDestination.Venue -> VenueDetailScreen(
                            onBack = { controller.navigateUp() },
                            onEventClick = openEvent,
                            favorites = favorites,
                            onFavoriteChange = { event, save -> favoritesViewModel.changeFavorite(event, save, offerUndo = !save) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingTabBar(selectedTab: Tab, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = shape, color = colors.surface, shadowElevation = 7.dp
    ) {
        Row(Modifier.fillMaxWidth().clip(shape).selectableGroup()) {
            Tab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val tint = if (selected) colors.primary else colors.onSurfaceVariant
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painterResource(if (selected) tab.filledIcon else tab.icon), null,
                        Modifier.size(24.dp), tint = tint)
                    Text(stringResource(tab.title), color = tint,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
