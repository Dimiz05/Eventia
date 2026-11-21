package com.example.eventia

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.domain.model.Account
import com.example.eventia.repositories.AccountField
import com.example.eventia.repositories.SessionRepository
import com.example.eventia.ui.screens.SessionForm
import com.example.eventia.ui.screens.SessionUiState
import com.example.eventia.ui.screens.SessionViewModel
import com.example.eventia.ui.theme.EventiaTheme
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionFlowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun invalidEmailAndShortPasswordShowSupportingTextInTheirFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = onMainSession()
        compose.setContent {
            val state by session.state.collectAsState()
            EventiaTheme {
                SessionForm(
                    form = state as SessionUiState.Form,
                    onSubmit = session::submit,
                    onSwitch = session::switchForm,
                    onEdit = session::clearError,
                    onGuest = session::continueAsGuest
                )
            }
        }
        val email = compose.onNode(hasSetTextAction() and hasText(context.getString(R.string.auth_email)))
        email.performTextInput(context.getString(R.string.app_name))
        compose.onNode(hasText(context.getString(R.string.auth_sign_in)) and hasClickAction()).performClick()

        compose.onNode(
            hasSetTextAction() and hasText(context.getString(R.string.auth_email)) and
                hasText(context.getString(R.string.auth_invalid_email))
        ).assertIsDisplayed()
        compose.onNode(
            hasSetTextAction() and hasText(context.getString(R.string.auth_password)) and
                hasText(context.getString(R.string.auth_short_password))
        ).assertIsDisplayed()
    }

    private fun onMainSession(): SessionViewModel {
        lateinit var session: SessionViewModel
        onMain { session = SessionViewModel(LocalSession(hasToken = false)) }
        return session
    }

    @Test
    fun figureTwoFieldsRejectBlankEmailAndShortPasswordLocally() = onMain {
        val session = SessionViewModel(LocalSession(hasToken = false))

        session.submit(name = "", email = "", password = "")

        assertEquals(
            SessionUiState.Form(
                errors = mapOf(
                    AccountField.Email to R.string.auth_invalid_email,
                    AccountField.Password to R.string.auth_short_password
                )
            ),
            session.state.value
        )
    }

    @Test
    fun cleanStartAllowsGuestEntryAndReturnToSignIn() = onMain {
        val repository = LocalSession(hasToken = false)
        val session = SessionViewModel(repository)

        assertEquals(SessionUiState.Form(), session.state.value)
        session.continueAsGuest()
        assertEquals(SessionUiState.Guest, session.state.value)
        assertFalse(repository.hasToken)
        assertEquals(0, repository.accountRequests)

        session.openSignIn()
        assertEquals(SessionUiState.Form(), session.state.value)
    }

    @Test
    fun offlineValidationKeepsSessionUntilLogout() = onMain {
        val repository = LocalSession(hasToken = true, offline = true)
        val session = SessionViewModel(repository)

        assertEquals(SessionUiState.Active(loading = false), session.state.value)
        assertTrue(repository.hasToken)
        session.retryAccount()
        assertEquals(2, repository.accountRequests)

        session.signOut()
        assertFalse(repository.hasToken)
        assertEquals(SessionUiState.Form(), session.state.value)
        assertEquals(SessionUiState.Form(), SessionViewModel(repository).state.value)
    }

    @Test
    fun rejectedRestoredSessionReturnsToLoginWithGuestAvailable() = onMain {
        val repository = LocalSession(hasToken = true)
        val session = SessionViewModel(repository)

        assertEquals(SessionUiState.Form(error = R.string.auth_expired), session.state.value)
        assertFalse(repository.hasToken)
        session.continueAsGuest()
        assertEquals(SessionUiState.Guest, session.state.value)
    }

    private fun onMain(test: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { test() }
    }

    private class LocalSession(var hasToken: Boolean, private val offline: Boolean = false) : SessionRepository {
        var accountRequests = 0
            private set

        override suspend fun hasSession() = hasToken

        override suspend fun signIn(email: String, password: String) = error("Unexpected sign-in")

        override suspend fun register(name: String, email: String, password: String) =
            error("Unexpected registration")

        override suspend fun loadAccount(): Account? {
            accountRequests++
            if (offline) throw IOException()
            hasToken = false
            return null
        }

        override suspend fun signOut() {
            hasToken = false
        }
    }
}
