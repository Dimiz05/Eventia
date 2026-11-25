package com.example.eventia

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.data.connectivity.AndroidCatalogConnectivity
import com.example.eventia.ui.navigation.EventiaNavigation
import com.example.eventia.ui.screens.session.LaunchSplash
import com.example.eventia.ui.screens.session.SessionForm
import com.example.eventia.ui.screens.session.SessionStartup
import com.example.eventia.ui.screens.session.SessionUiState
import com.example.eventia.ui.screens.session.SessionViewModel
import com.example.eventia.ui.theme.EventiaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val SplashDurationMillis = 2_000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectivity: AndroidCatalogConnectivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventiaTheme {
                Surface {
                    val session: SessionViewModel = hiltViewModel()
                    val navigationState = rememberSaveableStateHolder()
                    var splashDeadline by rememberSaveable {
                        mutableStateOf<Long?>(SystemClock.elapsedRealtime() + SplashDurationMillis)
                    }
                    LaunchedEffect(Unit) {
                        splashDeadline?.let { deadline ->
                            delay((deadline - SystemClock.elapsedRealtime()).coerceIn(0L, SplashDurationMillis))
                            splashDeadline = null
                        }
                    }
                    if (splashDeadline != null) {
                        LaunchSplash()
                        return@Surface
                    }

                    val state = session.state.collectAsStateWithLifecycle().value
                    val returnEventId = session.returnEventId
                    when (state) {
                        SessionUiState.Starting -> SessionStartup(false, session::retryStartup)
                        SessionUiState.StartupError -> SessionStartup(true, session::retryStartup)
                        is SessionUiState.Form -> {
                            if (returnEventId == null) {
                                LaunchedEffect(navigationState) { navigationState.removeState("catalog") }
                            } else {
                                BackHandler(onBack = session::continueAsGuest)
                            }
                            SessionForm(
                                state, session::submit, session::switchForm,
                                session::clearError, session::continueAsGuest
                            )
                        }
                        SessionUiState.Guest, is SessionUiState.Active -> {
                            navigationState.SaveableStateProvider("catalog") {
                                EventiaNavigation(
                                    currentSession = state as? SessionUiState.Active,
                                    onRetryAccount = session::retryAccount,
                                    onProfileSessionExpired = { session.expireSession() },
                                    onSignIn = session::openSignIn,
                                    onSignOut = session::signOut,
                                    online = connectivity.online,
                                    returnEventId = returnEventId,
                                    onEventReturnHandled = session::eventReturnHandled,
                                    onEventSignIn = session::requestEventSignIn,
                                    onEventSessionExpired = session::expireEventSession,
                                    onAttendanceChanged = session::retryAccount
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
