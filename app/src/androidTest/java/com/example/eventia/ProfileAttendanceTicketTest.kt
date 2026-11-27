package com.example.eventia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.eventia.domain.model.Account
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.profile.AttendanceGroups
import com.example.eventia.ui.screens.profile.ProfileScreen
import com.example.eventia.ui.screens.profile.ProfileUiState
import com.example.eventia.ui.screens.session.SessionUiState
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAttendanceTicketTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun twoTicketsFitAndManageOpensTheSelectedEvent() {
        val first = event(1, "Festival al aire libre", "Parque Urbano Central")
        val second = event(
            2,
            "Encuentro de música y artes escénicas al aire libre",
            "Centro Cultural Estación del Norte"
        )
        var openedEvent: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                EventiaTheme {
                    Box(Modifier.size(width = 360.dp, height = 700.dp)) {
                        ProfileScreen(
                            session = SessionUiState.Active(Account("camila@example.com", "Camila Rodríguez"), loading = false),
                            state = ProfileUiState(AttendanceGroups(listOf(first, second), emptyList()), loading = false),
                            onRetryAccount = {},
                            onSignIn = {},
                            onSignOut = {},
                            onRetryAttendances = {},
                            onEventClick = { openedEvent = it }
                        )
                    }
                }
            }
        }

        compose.onNodeWithText(first.title, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(second.title, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(second.venue.name, useUnmergedTree = true).assertIsDisplayed()
        val actions = compose.onAllNodesWithText("Gestionar inscripción")
        actions[0].assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        actions[1].assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(second.id, openedEvent) }
    }

    private fun event(id: Long, title: String, venue: String) = Event(
        id = id,
        title = title,
        summary = "",
        description = null,
        startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
        endsAt = null,
        category = Category(1, "Música"),
        imageUrl = null,
        venue = Venue(id, venue, ""),
        capacity = 50,
        availableSpots = 36,
        isAttending = true
    )
}
