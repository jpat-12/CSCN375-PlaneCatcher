package com.planecatcher.ui.quiz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.common.formatDuration
import com.planecatcher.ui.theme.color

@Composable
fun QuizScreen(
    onDone: () -> Unit,
    onOpenCollection: (String) -> Unit,
    vm: QuizViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val s = state) {
            QuizUiState.Loading -> Text("Loading question…")
            QuizUiState.NotFound -> Message("That plane has flown out of reach.", onDone)
            QuizUiState.AlreadyCaught -> Message("You've already caught this plane.", onDone)
            QuizUiState.MockLocation -> Message(
                "Mock location detected. Turn off location spoofing to catch planes, or use a Location Jump.",
                onDone,
            )
            is QuizUiState.Locked -> {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Text("This plane's quiz is locked", style = MaterialTheme.typography.headlineSmall)
                Text("Try again in ${formatDuration(s.retryAtMs - now)}. You can still try other planes.")
                Button(onClick = onDone) { Text("Back to radar") }
            }
            is QuizUiState.Asking -> Asking(s, vm::answer)
            is QuizUiState.Caught -> Caught(s, onDone = onDone, onView = { onOpenCollection(s.plane.hex) })
            is QuizUiState.Missed -> {
                Text("Not quite!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                Text(s.question.prompt, style = MaterialTheme.typography.titleMedium)
                Text("You answered: ${s.question.options[s.chosenIndex]}")
                Text("Correct answer: ${s.question.correctAnswer}", color = MaterialTheme.colorScheme.primary)
                Text(
                    "The plane got away. Its quiz is locked for ${formatDuration(s.retryAtMs - now)}, " +
                        "and you'll get a new question next time. Other planes are still fair game.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to radar") }
            }
        }
    }
}

@Composable
private fun Asking(s: QuizUiState.Asking, onAnswer: (Int) -> Unit) {
    val a = s.plane.aircraft
    PlanePhoto(s.photo?.url, s.plane.tier, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
    if (s.photo != null) {
        Text(
            "Photo © ${s.photo.photographer ?: "unknown"} · planespotters.net",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    TierBadge(s.plane.tier)
    Text(
        "Answer correctly to catch ${a.callsign ?: a.registration ?: "this plane"}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(s.question.prompt, style = MaterialTheme.typography.headlineSmall)
    s.question.options.forEachIndexed { i, option ->
        Button(
            onClick = { onAnswer(i) },
            enabled = !s.submitting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text("${'A' + i}.  $option", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun Caught(s: QuizUiState.Caught, onDone: () -> Unit, onView: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val spin = remember { Animatable(-180f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(250))
        spin.animateTo(0f, tween(600))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val p = s.plane
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.size(12.dp))
        Icon(
            Icons.Filled.Verified,
            contentDescription = null,
            tint = p.tier.color,
            modifier = Modifier.size(96.dp).scale(scale.value).rotate(spin.value).alpha(fade.value),
        )
        Text("Caught!", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        TierBadge(p.tier)
        Text(
            p.typeName ?: AircraftCatalog.lookup(p.typeCode)?.fullName ?: p.typeCode ?: "Unknown type",
            style = MaterialTheme.typography.titleLarge,
        )
        Text("+${p.points} ${if (p.points == 1) "point" else "points"}", style = MaterialTheme.typography.titleMedium)
        if (p.jumpCode != null) {
            Text(
                "Your Location Jump has ended. The next one unlocks in 24 hours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PlanePhoto(p.photoUrl, p.tier, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Button(onClick = onView, modifier = Modifier.fillMaxWidth()) { Text("View in collection") }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to radar") }
    }
}

@Composable
private fun Message(text: String, onDone: () -> Unit) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Button(onClick = onDone) { Text("Back to radar") }
}
