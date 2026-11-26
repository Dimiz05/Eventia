package com.example.eventia

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.favorites.FavoritesScreen
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savedCardShowsCountAndKeepsOpenAndRemoveSeparate() {
        val event = Event(
            id = 7,
            title = "Noche de jazz",
            summary = "",
            description = null,
            startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
            endsAt = null,
            category = Category(1, "Música"),
            imageUrl = null,
            venue = Venue(1, "Casa de la Cultura", "Av. Principal 123"),
            capacity = 50,
            availableSpots = 36,
            isAttending = null
        )
        val state = mutableStateOf(FavoritesUiState(events = listOf(event), ids = setOf(event.id)))
        val opened = mutableListOf<Long>()
        val removed = mutableListOf<Long>()
        compose.setContent {
            EventiaTheme {
                FavoritesScreen(
                    state = state.value,
                    onEventClick = { opened += it },
                    onRemove = { removed += it.id },
                    onRetry = {},
                    onExplore = {}
                )
            }
        }

        compose.onNodeWithText("1 guardado").assertIsDisplayed()
        compose.onNodeWithText("Ver detalle").assertDoesNotExist()
        compose.onNodeWithText(event.title).performClick()
        compose.onNodeWithContentDescription("Quitar de favoritos").performClick()
        compose.runOnIdle {
            assertEquals(listOf(event.id), opened)
            assertEquals(listOf(event.id), removed)
            state.value = state.value.copy(pendingIds = setOf(event.id))
        }
        compose.onNodeWithContentDescription("Quitar de favoritos").assertIsNotEnabled()
    }

    @Test
    fun emptyFavoritesOffersCatalogNavigation() {
        var opened = 0
        compose.setContent {
            EventiaTheme {
                FavoritesScreen(
                    FavoritesUiState(events = emptyList()),
                    onEventClick = {},
                    onRemove = {},
                    onRetry = {},
                    onExplore = { opened++ }
                )
            }
        }
        compose.onNodeWithText("Explorar eventos").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
