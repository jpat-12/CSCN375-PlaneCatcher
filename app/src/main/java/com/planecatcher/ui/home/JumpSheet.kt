package com.planecatcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.planecatcher.core.rules.JumpDestinations
import com.planecatcher.core.rules.JumpTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpSheet(onPick: (JumpTarget) -> Unit, onDismiss: () -> Unit) {
    var confirm by remember { mutableStateOf<JumpTarget?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Location Jump", style = MaterialTheme.typography.titleLarge)
            Text(
                "Move your radar to a busy airport for up to 3 hours. The jump ends as soon as you " +
                    "catch a plane with it. You can jump once every 24 hours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(JumpDestinations.all, key = { it.code }) { t ->
                ListItem(
                    headlineContent = { Text(t.name) },
                    leadingContent = { Text(t.code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth().clickable { confirm = t },
                )
                HorizontalDivider()
            }
        }
    }

    confirm?.let { t ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Jump to ${t.code}?") },
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
