package com.example.eventia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.eventia.R

@Composable
internal fun TabHeader(title: String, avatar: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, Modifier.weight(1f).padding(vertical = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium
        )
        avatar()
    }
}

@Composable
internal fun ProfileAvatar(signedIn: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.open_profile)
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).semantics {
        contentDescription = label
    }) {
        AccountAvatar(signedIn, Modifier.size(36.dp))
    }
}

@Composable
internal fun AccountAvatar(signedIn: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier, shape = CircleShape, color = colors.surface,
        border = BorderStroke(1.dp, colors.primary), contentColor = colors.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (signedIn) {
                Image(
                    painterResource(R.drawable.profile_avatar_duck), null,
                    Modifier.matchParentSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(painterResource(R.drawable.ic_profile_outline), null, Modifier.size(20.dp))
            }
        }
    }
}
