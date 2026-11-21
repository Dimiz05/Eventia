package com.example.eventia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.ui.navigation.EventiaNavigation
import com.example.eventia.ui.screens.SessionForm
import com.example.eventia.ui.screens.SessionStartup
import com.example.eventia.ui.screens.SessionUiState
import com.example.eventia.ui.screens.SessionViewModel
import com.example.eventia.ui.theme.EventiaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventiaTheme {
                val session: SessionViewModel = hiltViewModel()
                when (val state = session.state.collectAsStateWithLifecycle().value) {
                    SessionUiState.Starting -> SessionStartup(false, session::retryStartup)
                    SessionUiState.StartupError -> SessionStartup(true, session::retryStartup)
                    is SessionUiState.Form ->
                        SessionForm(
                            state, session::submit, session::switchForm,
                            session::clearError, session::continueAsGuest
                        )
                    SessionUiState.Guest, is SessionUiState.Active -> EventiaNavigation(
                        state as? SessionUiState.Active,
                        session::retryAccount,
                        session::openSignIn,
                        session::signOut
                    )
                }
            }
        }
    }
}
