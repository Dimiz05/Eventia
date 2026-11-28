package com.example.eventia.ui.screens.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun LaunchSplash() {
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(colorResource(R.color.eventia_splash_background))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("launchSplash"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.eventia_launch_gold),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.width(minOf(maxWidth * 0.94f, maxHeight, 480.dp))
        )
    }
}

@Preview(name = "Small phone", widthDp = 320, heightDp = 568)
@Preview(name = "Tall phone", widthDp = 393, heightDp = 873)
@Composable
private fun LaunchSplashPreview() {
    EventiaTheme { LaunchSplash() }
}
