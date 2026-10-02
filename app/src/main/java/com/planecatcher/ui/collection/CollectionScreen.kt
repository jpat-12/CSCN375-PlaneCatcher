package com.planecatcher.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.model.Tier
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatDate
import com.planecatcher.ui.theme.color

@Composable
fun CollectionScreen(onOpen: (String) -> Unit, vm: CollectionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Collection", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("${state.totalCaught}", "planes caught")
                    Stat("${state.totalPoints}", "points")
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.tierFilter == null,
                        onClick = { vm.setFilter(null) },
                        label = { Text("All") },
                    )
                    Tier.entries.forEach { tier ->
                        FilterChip(
                            selected = state.tierFilter == tier,
                            onClick = { vm.setFilter(if (state.tierFilter == tier) null else tier) },
                            label = { Text("${tier.displayName} (${state.countByTier[tier] ?: 0})") },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sort:", modifier = Modifier.padding(top = 6.dp))
                    CollectionSort.entries.forEach { s ->
                        FilterChip(selected = state.sort == s, onClick = { vm.setSort(s) }, label = { Text(s.label) })
                    }
                }
            }
        }

        if (state.loaded && state.planes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (state.totalCaught == 0) {
                        "No planes yet. Head to the radar, wait for a plane to fly over, and answer its question to catch it."
                    } else {
                        "No planes in this tier yet."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        items(state.planes, key = { it.hex }) { plane ->
            PlaneCard(plane, onClick = { onOpen(plane.hex) })
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaneCard(plane: CaughtPlaneEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(2.dp, plane.tier.color),
    ) {
        PlanePhoto(plane.photoUrl, null, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TierBadge(plane.tier)
            Text(
                plane.typeName ?: plane.typeCode ?: "Unknown type",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                plane.registration ?: plane.callsign ?: plane.hex.uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(formatDate(plane.caughtAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
