package com.planecatcher.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.model.Tier
import com.planecatcher.core.progress.ProgressSummary
import com.planecatcher.core.progress.SetProgress
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatDate
import com.planecatcher.ui.theme.color

private val tabs = listOf("Planes", "Book", "Sets", "Airlines")

@Composable
fun CollectionScreen(onOpen: (String) -> Unit, vm: CollectionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Collection", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("${state.totalCaught}", "planes")
                Stat("${progress?.typesCaught ?: 0}/${progress?.book?.size ?: 0}", "types")
                Stat("${progress?.totalPoints ?: state.totalPoints}", "points")
            }
        }
        TabRow(selectedTabIndex = tab, modifier = Modifier.padding(top = 8.dp)) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> PlanesTab(state, vm, onOpen)
            else -> progress?.let { p ->
                when (tab) {
                    1 -> BookTab(p)
                    2 -> SetsTab(p)
                    else -> AirlinesTab(p)
                }
            }
        }
    }
}

@Composable
private fun PlanesTab(state: CollectionUiState, vm: CollectionViewModel, onOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.tierFilter == null, onClick = { vm.setFilter(null) }, label = { Text("All") })
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

        items(state.planes, key = { it.hex }) { plane -> PlaneCard(plane, onClick = { onOpen(plane.hex) }) }
    }
}

/** Every known aircraft model: caught ones lit up, the rest as greyed-out silhouettes to hunt for. */
@Composable
private fun BookTab(p: ProgressSummary) {
    val entries = p.book.sortedWith(compareByDescending<com.planecatcher.core.progress.BookEntry> { it.caught }.thenBy { it.name })
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "${p.typesCaught} of ${p.book.size} aircraft types found. Planes not in this book can still be caught.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(entries, key = { it.name }) { e ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (e.caught) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = if (e.caught) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Flight,
                        contentDescription = null,
                        tint = if (e.caught) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        e.name,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (e.caught) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(e.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SetsTab(p: ProgressSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Finish a set to earn its bonus points. ${p.sets.count { it.complete }} of ${p.sets.size} complete.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(p.sets, key = { it.set.id }) { SetCard(it) }
    }
}

@Composable
private fun SetCard(sp: SetProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = if (sp.complete) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sp.set.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (sp.complete) "+${sp.set.bonus} earned" else "+${sp.set.bonus}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(sp.set.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { sp.have.toFloat() / sp.total }, modifier = Modifier.fillMaxWidth())
            sp.set.slots.zip(sp.filled).forEach { (slot, filled) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (filled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (filled) "Caught" else "Not yet",
                        tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("  ${slot.label}")
                }
            }
        }
    }
}

@Composable
private fun AirlinesTab(p: ProgressSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                if (p.airlines.isEmpty()) "Catch an airline flight to earn its badge."
                else "${p.airlines.size} airline badges",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(p.airlines, key = { it.airline }) { badge ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        badge.airline.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Text(badge.airline, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${badge.count} caught", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
        border = BorderStroke(2.dp, plane.tier.color),
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
            Text(plane.registration ?: plane.callsign ?: plane.hex.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(formatDate(plane.caughtAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
