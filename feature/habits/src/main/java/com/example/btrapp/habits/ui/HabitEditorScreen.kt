package com.example.btrapp.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.core.DaysMask
import com.example.btrapp.core.ui.dayLabel

private val emojiSuggestions = listOf("💧", "📚", "🏃", "🧘", "🥗", "😴", "✍️", "🎸", "🧹", "💊")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitEditorViewModel = viewModel(
        factory = habitsFactory { r, handle -> HabitEditorViewModel(r, handle) }
    ),
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (viewModel.isNew) "New habit" else "Edit habit",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.title = it.take(60) },
            label = { Text("Habit") },
            placeholder = { Text("Drink 2L of water") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Icon", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.emoji.isEmpty(),
                onClick = { viewModel.emoji = "" },
                label = { Text("None") },
            )
            emojiSuggestions.forEach { emoji ->
                FilterChip(
                    selected = viewModel.emoji == emoji,
                    onClick = { viewModel.emoji = emoji },
                    label = { Text(emoji) },
                )
            }
        }

        Text("Repeat on", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..7).forEach { day ->
                FilterChip(
                    selected = DaysMask.isDayOn(viewModel.daysMask, day),
                    onClick = { viewModel.toggleDay(day) },
                    label = { Text(dayLabel(day)) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { viewModel.daysMask = DaysMask.EVERY_DAY }, label = { Text("Every day") })
            AssistChip(onClick = { viewModel.daysMask = DaysMask.WEEKDAYS }, label = { Text("Weekdays") })
            AssistChip(onClick = { viewModel.daysMask = DaysMask.WEEKENDS }, label = { Text("Weekends") })
        }

        viewModel.validationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { viewModel.save(onDone) },
            enabled = viewModel.validationError == null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        if (!viewModel.isNew) {
            TextButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete habit") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${viewModel.title}\"?") },
            text = { Text("Its streak and check-in history go with it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDone)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
