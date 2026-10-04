package com.example.btrapp.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.core.ui.formatDays

@Composable
internal fun ManageHabitsScreen(
    onBack: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onNewHabit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageHabitsViewModel = viewModel(factory = habitsFactory { r, _ -> ManageHabitsViewModel(r) }),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Your habits", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (summaries.isEmpty()) {
                item { Text("No habits yet. Start with something small you can do every day.") }
            }
            items(summaries, key = { it.habit.id }) { summary ->
                val habit = summary.habit
                Card(onClick = { onEditHabit(habit.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(habit.emoji ?: "•", style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(habit.title, style = MaterialTheme.typography.titleMedium)
                            Text(formatDays(habit.daysMask), style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🔥 ${summary.currentStreak}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "best ${summary.bestStreak}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewHabit,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New habit") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}
