package com.planecatcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.rules.JumpDestinations
import com.planecatcher.core.rules.JumpTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpSheet(onPick: (JumpTarget) -> Unit, onDismiss: () -> Unit) {
    var confirm by remember { mutableStateOf<JumpTarget?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf<GeoPoint?>(null) }
    // Fully expanded so the map gets room and isn't fighting the sheet's drag gesture.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Location Jump", style = MaterialTheme.typography.titleLarge)
            Text(
                "Move your radar for up to 3 hours. It ends as soon as you catch a plane with it. " +
                    "You can jump once every 24 hours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Airports") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pick on map") })
        }
        if (tab == 0) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search city, airport or code") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            val results = JumpDestinations.search(query)
            LazyColumn(Modifier.heightIn(max = 480.dp).padding(bottom = 24.dp)) {
                if (results.isEmpty()) {
                    item { Text("No airports match \"$query\". Try the map instead.", modifier = Modifier.padding(20.dp)) }
                }
                items(results, key = { it.code }) { t ->
                    ListItem(
                        headlineContent = { Text(t.name) },
                        leadingContent = {
                            Text(t.code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.fillMaxWidth().clickable { confirm = t },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tap anywhere to drop a pin. Busy airports and big cities have the most planes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MapPicker(selected = pin, onPick = { pin = it }, modifier = Modifier.fillMaxWidth().height(380.dp))
                Text("Map © OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
                Row {
                    Button(
                        onClick = { pin?.let { confirm = JumpDestinations.custom(it) } },
                        enabled = pin != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (pin == null) "Tap the map first" else "Jump here") }
                }
            }
        }
    }

    confirm?.let { t ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (t.code == JumpDestinations.CUSTOM_CODE) "Jump to this spot?" else "Jump to ${t.code}?") },
            text = {
                Text(
                    "Your radar will show planes around ${t.name} for up to 3 hours, or until you catch one. " +
                        "Your next jump will be available 24 hours after this one ends.",
                )
            },
            confirmButton = { TextButton(onClick = { confirm = null; onPick(t) }) { Text("Jump") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}
