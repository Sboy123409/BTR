package com.example.btrapp.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val MAX_ROWS = 5

/** Today's checklist for the Home hub. Shows the first few items; the Habits tab has the rest. */
@Composable
fun TodayChecklistCard(
    onOpenHabits: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayChecklistViewModel = viewModel(
        factory = habitsFactory { r, _ -> TodayChecklistViewModel(r) }
    ),
) {
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val list = checklist ?: return

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (list.total > 0) {
                    Text(
                        "${list.done}/${list.total} done",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (list.total == 0) {
                Text(
                    "Nothing planned yet. Add a habit to build or a to-do for today.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenHabits) { Text("Plan your day") }
                return@Column
            }
            LinearProgressIndicator(
                progress = { list.done.toFloat() / list.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
            // Unfinished first, so what's left is what shows.
            val habits = list.habits.sortedBy { it.checked }
            val tasks = list.tasks.sortedBy { it.done }
            habits.take(MAX_ROWS).forEach { item ->
                HabitRow(
                    item = item,
                    enabled = true,
                    onCheckedChange = { viewModel.setHabitChecked(item.habit.id, list.epochDay, it) },
                )
            }
            tasks.take((MAX_ROWS - habits.size).coerceAtLeast(0)).forEach { task ->
                TaskRow(task = task, onDoneChange = { viewModel.setTaskDone(task.id, it) })
            }
            val hidden = list.total - MAX_ROWS
            TextButton(onClick = onOpenHabits, modifier = Modifier.align(Alignment.End)) {
                Text(if (hidden > 0) "See all (+$hidden)" else "Open habits")
            }
        }
    }
}
