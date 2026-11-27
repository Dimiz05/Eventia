package com.example.eventia.ui.screens.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventia.R
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun LaunchSplash() {
    val forest = Color(0xFF203D1D)
    Box(
        Modifier.fillMaxSize()
            .background(colorResource(R.color.eventia_splash_background))
            .testTag("launchSplash")
    ) {
        Image(
            painter = painterResource(R.drawable.eventia_launch_market),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val width = minOf(maxWidth * 0.84f, maxHeight * 0.49f, 440.dp)
            // A shorter viewport crops more sky, so raise the text to clear the trees.
            val heightRatio = (maxHeight / maxWidth).coerceIn(1.6f, 2.1f)
            val verticalBias = -0.73f + (heightRatio - 1.6f) * 0.18f
            Column(
                modifier = Modifier.align(BiasAlignment(0f, verticalBias)).width(width),
                verticalArrangement = Arrangement.spacedBy(width * 0.025f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.launch_welcome),
                    color = forest,
                    fontSize = (width.value * 0.09f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(R.drawable.eventia_launch_wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.width(width).aspectRatio(700f / 168f)
                )
                Text(
                    text = stringResource(R.string.launch_tagline),
                    color = forest,
                    fontSize = (width.value * 0.052f).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(name = "Small phone", widthDp = 320, heightDp = 568)
@Preview(name = "Tall phone", widthDp = 393, heightDp = 873)
@Composable
private fun LaunchSplashPreview() {
    EventiaTheme { LaunchSplash() }
}
