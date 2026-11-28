package com.example.eventia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.explore.ExploreContent
import com.example.eventia.ui.screens.explore.ExploreFilters
import com.example.eventia.ui.screens.explore.ExploreUiState
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreCatalogLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val music = Category(8, "Música")
    private val food = Category(9, "Gastronomía")
    private val art = Category(42, "Arte")
    private val events = (1L..4L).map { id ->
        Event(
            id, "Evento $id", "", null, OffsetDateTime.parse("2030-12-01T23:30-04:00").minusMinutes(id - 1),
            null, Category(10, "Cultura"), null, Venue(1, "Teatro municipal", "Calle Sucre"), 50, 10, null
        )
    }

    @Test
    fun fitsThreeEqualCardsAndScrollsToFourth() {
        showCatalog(height = 600, fontScale = 1f)
        compose.onNodeWithText("1 dic · 23:30").assertExists()
        val first = compose.onNodeWithText("Evento 1").assertIsDisplayed()
        val second = compose.onNodeWithText("Evento 2").assertIsDisplayed()
        val third = compose.onNodeWithText("Evento 3").assertIsDisplayed()
        val firstGap = second.getUnclippedBoundsInRoot().top - first.getUnclippedBoundsInRoot().top
        val secondGap = third.getUnclippedBoundsInRoot().top - second.getUnclippedBoundsInRoot().top
        assertEquals(firstGap.value, secondGap.value, 0.01f)
        assertTrue(third.getUnclippedBoundsInRoot().bottom < 600.dp)
        compose.onNodeWithText("Evento 4").assertDoesNotExist()
        compose.onNode(
            hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        ).performScrollToIndex(3)
        compose.onNodeWithText("Evento 4").assertIsDisplayed()
    }

    @Test
    fun quickFiltersAndExtraCategoriesUseRealIdentifiers() {
        showCatalog(height = 600, fontScale = 1f, width = 320)
        val filterRow = compose.onNode(
            hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
        )
        compose.onNodeWithText("Todos").assertIsSelected().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText(music.name).performClick().assertIsSelected()
        filterRow.performScrollToIndex(2)
        val foodChip = compose.onNodeWithText(food.name).assertIsDisplayed()
        assertTrue(foodChip.getUnclippedBoundsInRoot().right <= 320.dp)
        foodChip.performClick().assertIsSelected()
        filterRow.performScrollToIndex(3)
        compose.onNodeWithText("Filtros").assertIsDisplayed().performClick()
        compose.onNodeWithText(art.name).performClick()
        compose.onNodeWithText("Filtros · Arte").assertIsSelected()
        filterRow.performScrollToIndex(0)
        compose.onNodeWithText("Todos").performClick().assertIsSelected()
    }

    @Test
    fun compactCatalogWithLargeTextStillScrollsToLastEvent() {
        showCatalog(height = 360, fontScale = 1.5f)
        compose.onNode(
            hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        ).performScrollToIndex(3)
        compose.onNodeWithText("Evento 4").assertIsDisplayed()
    }

    private fun showCatalog(height: Int, fontScale: Float, width: Int = if (fontScale > 1f) 320 else 380) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                EventiaTheme {
                    var category by remember { mutableStateOf<Long?>(null) }
                    Box(Modifier.size(width = width.dp, height = height.dp)) {
                        ExploreContent(
                            state = ExploreUiState(
                                filters = ExploreFilters(categoryId = category),
                                content = ExploreUiState.Content(listOf(music, food, art), events),
                                isLoading = false
                            ),
                            onQueryChange = {},
                            onCategorySelect = { id -> category = id.takeUnless { it == category } },
                            onClearFilters = { category = null },
                            onRefresh = {},
                            onEventClick = {},
                            sections = {}
                        )
                    }
                }
            }
        }
    }
}
