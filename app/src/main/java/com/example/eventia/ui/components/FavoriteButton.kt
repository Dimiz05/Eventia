package com.example.eventia.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.eventia.R

@Composable
internal fun FavoriteButton(selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(enabled = enabled, onClick = onClick) {
        Icon(painterResource(favoriteIcon(selected)), stringResource(favoriteLabel(selected)))
    }
}

internal fun favoriteIcon(selected: Boolean): Int =
    if (selected) R.drawable.ic_favorites else R.drawable.ic_favorite_outline

internal fun favoriteLabel(selected: Boolean): Int =
    if (selected) R.string.favorite_remove else R.string.favorite_add
