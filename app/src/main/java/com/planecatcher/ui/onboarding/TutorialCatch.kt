package com.planecatcher.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.planecatcher.core.model.Tier
import com.planecatcher.feedback.Feedback
import com.planecatcher.feedback.Sfx
import com.planecatcher.ui.common.PlanePhoto
import com.planecatcher.ui.common.TierBadge
import com.planecatcher.ui.theme.color
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val feedback: Feedback) : ViewModel() {
    fun play(sfx: Sfx) = feedback.play(sfx)
}

private const val QUESTION = "How many engines does the Boeing 747-8 have?"
private val OPTIONS = listOf("2 engines", "3 engines", "4 engines", "6 engines")
private const val CORRECT = 2

/**
 * A practice catch with a pretend plane: pop-up, quiz, then the catch. Works with
 * no network or GPS, so it teaches the loop anywhere (including classroom demos).
 * Practice planes are not added to the collection.
 */
@Composable
fun TutorialCatch(onPlay: (Sfx) -> Unit, onDone: () -> Unit) {
    // 0 = pop-up, 1 = quiz, 2 = caught
    var stage by rememberSaveable { mutableIntStateOf(0) }
    var wrongPick by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Practice catch", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        when (stage) {
            0 -> {
                LaunchedEffect(Unit) { onPlay(Sfx.PING) }
                Text(
                    "When a plane flies within 10 miles, a pop-up like this appears.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Plane overhead!", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            TierBadge(Tier.LEGENDARY)
                        }
                        PlanePhoto(null, Tier.LEGENDARY, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                        Text("Boeing 747-8", style = MaterialTheme.typography.headlineSmall)
                        Text("TRAIN1 · 4.2 mi away · 36,000 ft", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Legendary planes are worth 100 points.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { stage = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Start Catch") }
                    }
                }
            }
            1 -> {
                Text(
                    "Answer one question about the plane to catch it.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(QUESTION, style = MaterialTheme.typography.headlineSmall)
                OPTIONS.forEachIndexed { i, option ->
                    Button(
                        onClick = {
                            if (i == CORRECT) {
                                onPlay(Sfx.CATCH)
                                stage = 2
                            } else {
                                onPlay(Sfx.MISS)
                                wrongPick = i
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wrongPick == i) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (wrongPick == i) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text("${'A' + i}.  $option", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                }
                wrongPick?.let {
                    Text(
                        "Not quite! In the real game the plane would get away and be locked for an hour. " +
                            "This is practice, so try again. Hint: it's the famous four-engine jumbo.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> {
                val scale = remember { Animatable(0.2f) }
                LaunchedEffect(Unit) {
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Tier.LEGENDARY.color,
                    modifier = Modifier.size(96.dp).scale(scale.value).align(Alignment.CenterHorizontally),
                )
                Text(
                    "Caught!",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "Real catches go into your collection, earn points, fill sets and count toward daily challenges. " +
                        "Practice planes don't count, so go find a real one!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
        }
        if (stage < 2) {
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Skip practice") }
        }
    }
}
