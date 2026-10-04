package com.example.btrapp.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.core.ui.dayLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dayTitleFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** The Habits tab: one day's habits and to-dos, with a week strip to move between days. */
@Composable
internal fun HabitsScreen(
    onNewHabit: () -> Unit,
    onManageHabits: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = viewModel(factory = habitsFactory { r, _ -> HabitsViewModel(r) }),
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val today = viewModel.today
    val isFuture = selectedDay > today
    var newTask by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (selectedDay) {
                        today -> "Today"
                        today - 1 -> "Yesterday"
                        today + 1 -> "Tomorrow"
                        else -> LocalDate.ofEpochDay(selectedDay).format(dayTitleFormatter)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onManageHabits) {
                    Icon(Icons.Default.Edit, contentDescription = "Manage habits")
                }
            }
        }
        item {
            WeekStrip(
                selectedDay = selectedDay,
                today = today,
                onSelect = { viewModel.selectedDay.value = it },
            )
        }
        checklist?.let { list ->
            if (list.total > 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${list.done} of ${list.total} done",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        LinearProgressIndicator(
                            progress = { list.done.toFloat() / list.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                SectionHeader("Habits") {
                    TextButton(onClick = onNewHabit) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("New habit")
                    }
                }
            }
            if (list.habits.isEmpty()) {
                item {
                    Text(
                        "No habits due this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(list.habits, key = { "h${it.habit.id}" }) { item ->
                HabitRow(
                    item = item,
                    enabled = !isFuture,
                    onCheckedChange = { viewModel.setHabitChecked(item.habit.id, selectedDay, it) },
                )
            }
            if (isFuture && list.habits.isNotEmpty()) {
                item {
                    Text(
                        "Habits can be ticked off on the day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { SectionHeader("To-dos") {} }
            item {
                val add = {
                    viewModel.addTask(newTask)
                    newTask = ""
                }
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    placeholder = { Text("Add a to-do") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    trailingIcon = {
                        IconButton(onClick = add, enabled = newTask.isNotBlank()) {
                            Icon(Icons.Default.Add, contentDescription = "Add to-do")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(list.tasks, key = { "t${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    onDoneChange = { viewModel.setTaskDone(task.id, it) },
                    trailing = {
                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete to-do")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action()
    }
}

/** Mon–Sun of the selected day's week, with arrows to page between weeks. */
@Composable
private fun WeekStrip(selectedDay: Long, today: Long, onSelect: (Long) -> Unit) {
    val selected = LocalDate.ofEpochDay(selectedDay)
    val monday = selected.minusDays(selected.dayOfWeek.value - 1L).toEpochDay()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(selectedDay - 7) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0 until 7).forEach { offset ->
                val day = monday + offset
                val isSelected = day == selectedDay
                val colors = MaterialTheme.colorScheme
                Surface(
                    onClick = { onSelect(day) },
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> colors.primary
                        day == today -> colors.secondaryContainer
                        else -> colors.surfaceContainerHigh
                    },
                    contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(dayLabel(offset + 1).take(1), style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${LocalDate.ofEpochDay(day).dayOfMonth}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (day == today) FontWeight.Bold else null,
                        )
                    }
                }
            }
        }
        IconButton(onClick = { onSelect(selectedDay + 7) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}
