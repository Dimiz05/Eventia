package com.example.eventia

import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchSplashTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun launchShowsBrandBeforeOpeningTheSignInForm() {
        compose.mainClock.autoAdvance = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithTag("launchSplash").assertIsDisplayed()
            compose.onNodeWithContentDescription("Eventia").assertIsDisplayed()
            compose.onAllNodesWithText("Iniciar sesión").assertCountEquals(0)
            compose.onAllNodesWithText("Cargando sesión…").assertCountEquals(0)

            compose.mainClock.advanceTimeBy(1_000)
            compose.mainClock.autoAdvance = true
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Iniciar sesión").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("launchSplash").assertDoesNotExist()

            scenario.recreate()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithTag("launchSplash").assertDoesNotExist()
            compose.onNodeWithContentDescription("Correo electrónico").assertIsDisplayed()
        }
    }
}
