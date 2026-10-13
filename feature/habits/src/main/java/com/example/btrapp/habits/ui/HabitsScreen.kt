package com.example.btrapp.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.core.ui.dayLabel
import com.example.btrapp.habits.data.DayTask
import com.example.btrapp.habits.data.HabitItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

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
    val palette = todoPalette()
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var adding by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Header(selectedDay = selectedDay, today = today, onManageHabits = onManageHabits, palette = palette)
        }
        item {
            WeekStrip(
                selectedDay = selectedDay,
                today = today,
                onSelect = { viewModel.selectedDay.value = it },
                palette = palette,
            )
        }
        val list = checklist ?: return@LazyColumn
        if (list.total > 0) {
            item { ProgressCard(done = list.done, total = list.total, palette = palette) }
        }

        val openHabits = list.habits.filterNot { it.checked }
        val openTasks = list.tasks.filterNot { it.done }
        val doneHabits = list.habits.filter { it.checked }
        val doneTasks = list.tasks.filter { it.done }

        item {
            SectionHeader("To do · ${openHabits.size + openTasks.size}", palette) {
                SectionAction("New habit", palette, icon = true, onClick = onNewHabit)
            }
        }
        item {
            if (openHabits.isEmpty() && openTasks.isEmpty()) {
                Text(
                    if (list.total == 0) "Nothing planned yet. Add a task or a habit to build."
                    else "All done. Enjoy the quiet.",
                    fontSize = 15.sp,
                    color = palette.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(palette.card)
                        .padding(20.dp),
                )
            } else {
                TodoCard(palette, openHabits + openTasks) { entry ->
                    ChecklistEntryRow(entry, !isFuture, viewModel, selectedDay, palette)
                }
            }
        }
        if (isFuture && list.habits.isNotEmpty()) {
            item {
                Text("Habits can be ticked off on the day.", fontSize = 13.sp, color = palette.muted)
            }
        }

        val doneCount = doneHabits.size + doneTasks.size
        if (doneCount > 0) {
            item {
                SectionHeader("Completed · $doneCount", palette) {
                    SectionAction(if (showCompleted) "Hide" else "Show", palette) { showCompleted = !showCompleted }
                }
            }
            if (showCompleted) {
                item {
                    TodoCard(palette, doneHabits + doneTasks) { entry ->
                        ChecklistEntryRow(entry, !isFuture, viewModel, selectedDay, palette)
                    }
                }
            }
        }

        item {
            Button(
                onClick = { adding = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(56.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a task", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (adding) {
        AddTaskDialog(
            onAdd = {
                viewModel.addTask(it)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun Header(selectedDay: Long, today: Long, onManageHabits: () -> Unit, palette: TodoPalette) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "One thing at a time.",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onManageHabits,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.card),
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Manage habits", tint = palette.muted, modifier = Modifier.size(20.dp))
            }
        }
        val date = LocalDate.ofEpochDay(selectedDay).format(dateFormatter)
        val relative = when (selectedDay) {
            today -> null
            today - 1 -> "Yesterday"
            today + 1 -> "Tomorrow"
            else -> null
        }
        Text(relative?.let { "$it · $date" } ?: date, fontSize = 15.sp, color = palette.muted)
    }
}

/** Mon–Sun of the selected day's week as a segmented control, with arrows to page between weeks. */
@Composable
private fun WeekStrip(selectedDay: Long, today: Long, onSelect: (Long) -> Unit, palette: TodoPalette) {
    val selected = LocalDate.ofEpochDay(selectedDay)
    val monday = selected.minusDays(selected.dayOfWeek.value - 1L).toEpochDay()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.segmentTrack)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", palette) { onSelect(selectedDay - 7) }
        (0 until 7).forEach { offset ->
            val day = monday + offset
            val isSelected = day == selectedDay
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) palette.card else palette.segmentTrack)
                    .clickable { onSelect(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val color = if (isSelected || day == today) palette.accent else palette.muted
                Text(dayLabel(offset + 1).take(1), fontSize = 11.sp, color = color)
                Text(
                    "${LocalDate.ofEpochDay(day).dayOfMonth}",
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || day == today) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected || day == today) palette.accent else palette.ink,
                )
            }
        }
        WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", palette) { onSelect(selectedDay + 7) }
    }
}

@Composable
private fun WeekArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    palette: TodoPalette,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(32.dp, 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = palette.muted)
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, palette: TodoPalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.accentSoft)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "A little progress, every day.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = palette.accent,
                modifier = Modifier.weight(1f),
            )
            Text("$done of $total done", fontSize = 13.sp, color = palette.accent)
        }
        LinearProgressIndicator(
            progress = { done.toFloat() / total },
            color = palette.accent,
            trackColor = palette.card,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String, palette: TodoPalette, action: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = palette.ink, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun SectionAction(label: String, palette: TodoPalette, icon: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        if (icon) {
            Icon(Icons.Default.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, fontSize = 15.sp, color = palette.accent)
    }
}

/** A habit or a to-do; the two share the open and completed cards. */
@Composable
private fun ChecklistEntryRow(
    entry: Any,
    habitsEnabled: Boolean,
    viewModel: HabitsViewModel,
    day: Long,
    palette: TodoPalette,
) {
    when (entry) {
        is HabitItem -> HabitTodoRow(entry, habitsEnabled, viewModel, day, palette)
        is DayTask -> TaskTodoRow(entry, viewModel, palette)
    }
}

@Composable
private fun HabitTodoRow(
    item: HabitItem,
    enabled: Boolean,
    viewModel: HabitsViewModel,
    day: Long,
    palette: TodoPalette,
) {
    TodoRow(
        title = listOfNotNull(item.habit.emoji, item.habit.title).joinToString(" "),
        done = item.checked,
        enabled = enabled,
        onToggle = { viewModel.setHabitChecked(item.habit.id, day, it) },
        palette = palette,
        tag = "Habit",
        tagHighlighted = true,
        detail = if (item.streak > 0) "🔥 ${item.streak}-day streak" else "Start a streak",
    )
}

@Composable
private fun TaskTodoRow(task: DayTask, viewModel: HabitsViewModel, palette: TodoPalette) {
    TodoRow(
        title = task.title,
        done = task.done,
        enabled = true,
        onToggle = { viewModel.setTaskDone(task.id, it) },
        palette = palette,
        tag = "To-do",
        tagHighlighted = false,
        detail = null,
        trailing = {
            IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                Icon(Icons.Default.Close, contentDescription = "Delete to-do", tint = palette.muted, modifier = Modifier.size(18.dp))
            }
        },
    )
}

@Composable
private fun AddTaskDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val add = { if (text.isNotBlank()) onAdd(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a task") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = add, enabled = text.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
