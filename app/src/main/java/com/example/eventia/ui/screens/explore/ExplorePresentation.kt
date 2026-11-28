package com.example.eventia.ui.screens.explore

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.eventia.R
import com.example.eventia.ui.theme.EventiaTheme
import com.example.eventia.ui.components.TabHeader
import kotlinx.coroutines.delay

@Composable
internal fun exploreColors() = MaterialTheme.colorScheme

@Composable
internal fun Modifier.panoramicCaptionScrim(): Modifier {
    val fade = 5.dp
    return background(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
            endY = with(LocalDensity.current) { fade.toPx() }
        )
    ).padding(top = fade)
}

@Composable
internal fun ExploreHeader(avatar: @Composable () -> Unit = {}) {
    TabHeader(stringResource(R.string.tab_explore), avatar)
}

@Composable
internal fun ExploreSections(showVenues: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .background(colors.primaryContainer, CircleShape).padding(2.dp).selectableGroup()
    ) {
        listOf(false, true).forEach { venues ->
            val selected = showVenues == venues
            val contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant
            Row(
                Modifier.weight(1f).clip(CircleShape)
                    .then(if (selected) Modifier.background(colors.primary) else Modifier)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(venues) })
                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (venues) R.drawable.ic_catalog_buildings else R.drawable.ic_catalog_ticket
                val label = if (venues) R.string.venues_title else R.string.events_title
                Icon(painterResource(icon), null, Modifier.size(18.dp), tint = contentColor)
                Text(
                    stringResource(label),
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun ExploreSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    inlineHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val container = colors.surface
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .then(if (inlineHint) Modifier.semantics { contentDescription = label } else Modifier),
        label = if (inlineHint) null else { { Text(label) } },
        placeholder = if (inlineHint) {
            { Text(label, color = colors.onSurfaceVariant) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        shape = CircleShape,
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_catalog_search), null, tint = colors.primary)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_catalog_close),
                        stringResource(R.string.venue_clear_search),
                        tint = colors.primary
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            cursorColor = colors.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExplorePullToRefresh(
    onRefresh: () -> Unit,
    isLoading: Boolean,
    hasScrollableContent: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val pullState = rememberPullToRefreshState()
    val isPulling by remember(pullState) { derivedStateOf { pullState.distanceFraction > 0f } }
    var showIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(showIndicator) {
        if (showIndicator) {
            delay(600)
            showIndicator = false
        }
    }
    Box(
        Modifier.fillMaxSize().pullToRefresh(
            isRefreshing = showIndicator,
            state = pullState,
            enabled = !isLoading,
            onRefresh = {
                showIndicator = true
                onRefresh()
            }
        )
    ) {
        val scrollModifier = if (hasScrollableContent) Modifier else {
            // Empty and error screens still dispatch the drag to the pull-to-refresh parent.
            Modifier.scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
        }
        Column(Modifier.fillMaxSize().then(scrollModifier), content = content)
        if (isPulling || showIndicator) {
            val label = stringResource(R.string.catalog_refresh)
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = showIndicator,
                modifier = Modifier.align(Alignment.TopCenter).semantics { contentDescription = label },
                containerColor = colors.primaryContainer,
                color = colors.onPrimaryContainer
            )
        }
    }
}

@Composable
internal fun ExploreCard(
    imageUrl: String?,
    @DrawableRes fallback: Int,
    onClick: () -> Unit,
    cover: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface)
    ) {
        val hasImage = !imageUrl.isNullOrBlank()
        Box(
            Modifier.fillMaxWidth().height(if (hasImage) 176.dp else 144.dp)
                .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainerHighest))),
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(fallback), null, Modifier.size(30.dp), tint = colors.primary)
                }
            }
            if (hasImage) {
                AsyncImage(imageUrl, contentDescription = null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
            cover()
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
internal fun ExploreBadge(
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(modifier, shape = CircleShape, color = containerColor, contentColor = contentColor) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun ExploreMetadata(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(painterResource(icon), null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ExplorePreviewFrame(content: @Composable () -> Unit) {
    EventiaTheme {
        MaterialTheme(colorScheme = exploreColors()) {
            Surface {
                Column(Modifier.statusBarsPadding()) {
                    ExploreHeader()
                    content()
                }
            }
        }
    }
}
