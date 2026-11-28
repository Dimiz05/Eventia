package com.example.eventia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun Modifier.loadingSemantics(label: String) = semantics {
    contentDescription = label
    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
}

@Composable
internal fun SkeletonBlock(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)))
}

@Composable
internal fun LoadingCard(label: String? = null) {
    val status = if (label == null) Modifier else Modifier.loadingSemantics(label)
    Surface(status.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.32f).height(16.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.78f).height(22.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
        }
    }
}

@Composable
internal fun LoadingScreen(label: String, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize().loadingSemantics(label).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(3) { LoadingCard() }
    }
}

@Composable
internal fun DetailSkeleton(label: String, coverHeight: Dp) {
    LazyColumn(Modifier.fillMaxSize().loadingSemantics(label)) {
        item {
            SkeletonBlock(Modifier.fillMaxWidth().height(coverHeight))
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth(0.28f).height(22.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.82f).height(28.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.60f).height(20.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.80f).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(124.dp))
            }
        }
    }
}

@Composable
internal fun MapSkeleton(label: String) {
    Box(Modifier.fillMaxSize().loadingSemantics(label)
        .background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(Modifier.weight(1f).height(56.dp))
                Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { SkeletonBlock(Modifier.weight(1f).height(36.dp)) }
            }
        }
    }
}
