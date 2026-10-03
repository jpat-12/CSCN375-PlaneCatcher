package com.planecatcher.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.planecatcher.core.progress.ChallengeProgress
import com.planecatcher.core.progress.ProgressSummary
import com.planecatcher.domain.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(repo: ProgressRepository) : ViewModel() {
    val summary: StateFlow<ProgressSummary?> = repo.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private val StreakOrange = Color(0xFFFF8A3D)

@Composable
fun ProgressScreen(vm: ProgressViewModel = hiltViewModel()) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val p = summary ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium)

        SectionCard {
            Text("Level ${p.level.level}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(p.level.title, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { p.level.fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(
                "${p.level.xpIntoLevel} / ${p.level.xpForNext} XP to level ${p.level.level + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = StreakOrange, modifier = Modifier.size(40.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "${p.streakDays}-day streak",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        when {
                            p.streakDays == 0 -> "Catch a plane today to start a streak."
                            !p.caughtToday -> "Catch a plane today to keep it going!"
                            else -> "You've caught a plane today. See you tomorrow!"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Best: ${p.bestStreakDays} days", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        SectionCard {
            Text("Today's challenges", style = MaterialTheme.typography.titleLarge)
            Text("New challenges every day at midnight.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.today.forEach { ChallengeRow(it) }
        }

        SectionCard {
            Text("Points", style = MaterialTheme.typography.titleLarge)
            PointsRow("From catches", p.catchPoints)
            PointsRow("Set bonuses", p.setBonus)
            PointsRow("Challenge rewards", p.challengeBonus)
            HorizontalDivider()
            PointsRow("Total", p.totalPoints, bold = true)
        }
    }
}

@Composable
private fun ChallengeRow(c: ChallengeProgress) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            if (c.complete) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (c.complete) "Done" else "Not done",
            tint = if (c.complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(c.challenge.title, style = MaterialTheme.typography.bodyLarge)
            if (c.challenge.goal > 1) {
                Text("${c.progress} / ${c.challenge.goal}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("+${c.challenge.reward}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PointsRow(label: String, value: Int, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text("$value", style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
