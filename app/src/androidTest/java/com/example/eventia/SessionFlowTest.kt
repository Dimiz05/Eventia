package com.example.eventia

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.domain.model.Account
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.AccountField
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.repositories.EventSessionExpired
import com.example.eventia.repositories.SessionRepository
import com.example.eventia.ui.navigation.DetailId
import com.example.eventia.ui.screens.eventdetail.AttendanceConflict
import com.example.eventia.ui.screens.eventdetail.EventDetailScreen
import com.example.eventia.ui.screens.eventdetail.EventDetailUiState
import com.example.eventia.ui.screens.eventdetail.EventDetailViewModel
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.screens.session.SessionForm
import com.example.eventia.ui.screens.session.SessionUiState
import com.example.eventia.ui.screens.session.SessionViewModel
import com.example.eventia.ui.theme.EventiaTheme
import java.io.IOException
import java.time.OffsetDateTime
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response

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
        onMain { session = SessionViewModel(LocalSession(hasToken = false), SavedStateHandle()) }
        return session
    }

    @Test
    fun figureTwoFieldsRejectBlankEmailAndShortPasswordLocally() = onMain {
        val session = SessionViewModel(LocalSession(hasToken = false), SavedStateHandle())

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
        val session = SessionViewModel(repository, SavedStateHandle())

        assertEquals(SessionUiState.Form(), session.state.value)
        session.continueAsGuest()
        assertEquals(SessionUiState.Guest, session.state.value)
        assertFalse(repository.hasToken)
        assertEquals(0, repository.accountRequests)

        session.openSignIn()
        assertEquals(SessionUiState.Form(), session.state.value)
    }

    @Test
    fun cancellingEventSignInReturnsAsGuestAndKeepsTheEventUntilNavigationConsumesIt() = onMain {
        val repository = LocalSession(hasToken = false)
        val session = SessionViewModel(repository, SavedStateHandle())
        session.continueAsGuest()

        session.requestEventSignIn(8L)
        assertEquals(SessionUiState.Form(), session.state.value)
        assertEquals(8L, session.returnEventId)

        session.continueAsGuest()
        assertEquals(SessionUiState.Guest, session.state.value)
        assertFalse(repository.hasToken)
        assertEquals(8L, session.returnEventId)
        session.eventReturnHandled(8L)
        assertEquals(null, session.returnEventId)
    }

    @Test
    fun expiredAttendanceClearsTheTokenAndOpensSignInWithTheReturnEvent() = onMain {
        val repository = LocalSession(hasToken = true, offline = true)
        val session = SessionViewModel(repository, SavedStateHandle())
        val catalog = AttendanceCatalog()
        val detail = EventDetailViewModel(
            SavedStateHandle(mapOf(DetailId to catalog.event.id)), catalog, repository
        )

        detail.load(authenticated = true, onSessionExpired = session::expireEventSession)
        assertTrue(catalog.authenticatedRequest)
        detail.attend(session::expireEventSession)

        assertEquals(catalog.event.id, repository.attendanceEventId)
        assertEquals(SessionUiState.Form(error = R.string.auth_expired), session.state.value)
        assertFalse(repository.hasToken)
        assertEquals(catalog.event.id, session.returnEventId)
    }

    @Test
    fun expiredProfileSessionClearsTheTokenWithoutAnEventToReturnTo() = onMain {
        val repository = LocalSession(hasToken = true)
        val session = SessionViewModel(repository, SavedStateHandle())

        session.expireSession()

        assertEquals(SessionUiState.Form(error = R.string.auth_expired), session.state.value)
        assertFalse(repository.hasToken)
        assertEquals(null, session.returnEventId)
    }

    @Test
    fun attendanceUpdatesDetailAndRequestsProfileRefreshAfterEachChange() = onMain {
        val catalog = AttendanceCatalog()
        val original = catalog.event
        val repository = LocalSession(hasToken = true)
        repository.onAttend = {
            catalog.event = original.copy(isAttending = true, availableSpots = original.availableSpots - 1)
        }
        repository.onCancel = {
            assertEquals(original.id, it)
            catalog.event = original
        }
        val detail = EventDetailViewModel(
            SavedStateHandle(mapOf(DetailId to original.id)), catalog, repository
        )
        val onExpired: (Long) -> Unit = { error("Unexpected session expiration") }
        var profileRefreshes = 0
        detail.load(true, onExpired)

        detail.attend(onExpired) { profileRefreshes++ }
        assertEquals(original.id, repository.attendanceEventId)
        assertEquals(1, profileRefreshes)
        assertFalse(catalog.allowedSavedDetail)
        assertEquals(catalog.event, (detail.state.value as EventDetailUiState.Content).event)

        detail.attend(onExpired) { profileRefreshes++ }
        assertEquals(2, profileRefreshes)
        assertFalse(catalog.allowedSavedDetail)
        assertEquals(original, (detail.state.value as EventDetailUiState.Content).event)
    }

    @Test
    fun pastEventsRejectBothAttendanceChanges() = onMain {
        val catalog = AttendanceCatalog()
        val pastEvent = catalog.event.copy(startsAt = OffsetDateTime.now().minusDays(1))
        val onExpired: (Long) -> Unit = { error("Unexpected session expiration") }

        for (attending in listOf(false, true)) {
            catalog.event = pastEvent.copy(isAttending = attending)
            val repository = LocalSession(hasToken = true)
            var requests = 0
            var refreshes = 0
            repository.onAttend = { requests++ }
            repository.onCancel = { requests++ }
            val detail = EventDetailViewModel(
                SavedStateHandle(mapOf(DetailId to pastEvent.id)), catalog, repository
            )
            detail.load(true, onExpired)
            val loaded = detail.state.value

            detail.attend(onExpired) { refreshes++ }

            assertEquals(0, requests)
            assertEquals(0, refreshes)
            assertEquals(loaded, detail.state.value)
        }
    }

    @Test
    fun pastEventDetailShowsWhyAttendanceCannotBeChanged() {
        val catalog = AttendanceCatalog()
        catalog.event = catalog.event.copy(startsAt = OffsetDateTime.now().minusDays(1), isAttending = true)
        val detail = EventDetailViewModel(
            SavedStateHandle(mapOf(DetailId to catalog.event.id)), catalog, LocalSession(hasToken = true)
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            EventiaTheme {
                EventDetailScreen(
                    authenticated = true,
                    onEventSignIn = { error("Unexpected sign-in") },
                    onSessionExpired = { error("Unexpected session expiration") },
                    onBack = {},
                    onExplore = {},
                    onAttendanceChanged = { error("Unexpected attendance change") },
                    onVenueClick = {},
                    favorites = FavoritesUiState(),
                    onFavoriteChange = { _, _ -> },
                    viewModel = detail
                )
            }
        }

        compose.onNode(hasText(context.getString(R.string.event_cancel_attendance)))
            .performScrollTo().assertIsNotEnabled()
        compose.onNode(hasText("Este evento ya pasó y no puedes cambiar tu asistencia."))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun conflictUsesUpdatedAttendanceBeforeCapacityAndDoesNotRetryThePost() = onMain {
        val catalog = AttendanceCatalog()
        val original = catalog.event
        val onExpired: (Long) -> Unit = { error("Unexpected session expiration") }
        for ((alreadyRegistered, expected) in listOf(
            false to AttendanceConflict.Full, true to AttendanceConflict.AlreadyRegistered
        )) {
            catalog.event = original
            val repository = LocalSession(hasToken = true)
            var posts = 0
            repository.onAttend = {
                posts++
                catalog.event = original.copy(isAttending = alreadyRegistered, availableSpots = 0)
                throw HttpException(Response.error<Void>(409, "".toResponseBody()))
            }
            val detail = EventDetailViewModel(
                SavedStateHandle(mapOf(DetailId to original.id)), catalog, repository
            )
            detail.load(true, onExpired)

            detail.attend(onExpired) { error("Unexpected profile refresh") }

            val state = detail.state.value as EventDetailUiState.Content
            assertEquals(1, posts)
            assertTrue(catalog.authenticatedRequest)
            assertFalse(catalog.allowedSavedDetail)
            assertEquals(catalog.event, state.event)
            assertEquals(expected, state.conflict)
            assertFalse(state.updating)
        }
    }

    @Test
    fun offlineValidationKeepsSessionUntilLogout() = onMain {
        val repository = LocalSession(hasToken = true, offline = true)
        val session = SessionViewModel(repository, SavedStateHandle())

        assertEquals(SessionUiState.Active(loading = false), session.state.value)
        assertTrue(repository.hasToken)
        session.retryAccount()
        assertEquals(2, repository.accountRequests)

        session.signOut()
        assertFalse(repository.hasToken)
        assertEquals(SessionUiState.Form(), session.state.value)
        assertEquals(SessionUiState.Form(), SessionViewModel(repository, SavedStateHandle()).state.value)
    }

    @Test
    fun rejectedRestoredSessionReturnsToLoginWithGuestAvailable() = onMain {
        val repository = LocalSession(hasToken = true)
        val session = SessionViewModel(repository, SavedStateHandle())

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
        var attendanceEventId: Long? = null
            private set
        var onAttend: (Long) -> Unit = { throw EventSessionExpired() }
        var onCancel: (Long) -> Unit = { error("Unexpected cancellation") }

        override suspend fun hasSession() = hasToken

        override suspend fun signIn(email: String, password: String) = error("Unexpected sign-in")

        override suspend fun register(name: String, email: String, password: String) =
            error("Unexpected registration")
        override suspend fun attendEvent(id: Long) {
            attendanceEventId = id
            onAttend(id)
        }
        override suspend fun cancelAttendance(id: Long) = onCancel(id)

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

    private class AttendanceCatalog : CatalogRepository {
        var event = Event(
            id = 42,
            title = "Noche de jazz en la plaza",
            summary = "Clásicos del jazz con músicos invitados.",
            description = null,
            startsAt = OffsetDateTime.now().plusDays(1),
            endsAt = null,
            category = Category(3, "Música"),
            imageUrl = null,
            venue = Venue(7, "Casa de la Cultura", "Av. Principal 123", latitude = -17.3935),
            capacity = 50,
            availableSpots = 36,
            isAttending = false
        )
        var authenticatedRequest = false
            private set
        var allowedSavedDetail = true
            private set

        override suspend fun getEvent(id: Long, authenticated: Boolean, allowSaved: Boolean): Event {
            assertEquals(event.id, id)
            authenticatedRequest = authenticated
            allowedSavedDetail = allowSaved
            return event
        }

        override suspend fun getEvents(query: String?, categoryId: Long?) = error("Unexpected catalog request")
        override suspend fun loadEvents(query: String?, categoryId: Long?) = error("Unexpected catalog request")
        override suspend fun getVenues() = error("Unexpected venues request")
        override suspend fun getVenue(id: Long) = error("Unexpected venue request")
        override suspend fun getVenueEvents(id: Long) = error("Unexpected venue events request")
    }
}
