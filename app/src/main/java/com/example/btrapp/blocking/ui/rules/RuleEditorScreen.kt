package com.example.btrapp.blocking.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.blocking.data.RuleType
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.ui.common.AppIcon
import com.example.btrapp.blocking.ui.common.containerFactory
import com.example.btrapp.blocking.ui.common.dayLabel
import com.example.btrapp.blocking.ui.common.formatMinuteOfDay
import com.example.btrapp.blocking.ui.common.formatMinutes
import com.example.btrapp.blocking.ui.common.isDebuggable

private val sessionOptions = listOf(15, 30, 60, 120, 240, 480)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuleEditorViewModel = viewModel(
        factory = containerFactory { c, handle -> RuleEditorViewModel(c.blockingRepository, handle) }
    ),
) {
    val context = LocalContext.current
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    var editingStart by rememberSaveable { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (viewModel.isNew) "New lock rule" else "Edit lock rule",
            style = MaterialTheme.typography.headlineSmall,
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RuleType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = viewModel.type == type,
                    onClick = { viewModel.type = type },
                    shape = SegmentedButtonDefaults.itemShape(index, RuleType.entries.size),
                ) {
                    Text(if (type == RuleType.SESSION) "One-off session" else "Recurring schedule")
                }
            }
        }

        when (viewModel.type) {
            RuleType.SESSION -> {
                Text("Lock for", style = MaterialTheme.typography.titleMedium)
                val options =
                    if (context.isDebuggable()) listOf(1) + sessionOptions else sessionOptions
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { minutes ->
                        FilterChip(
                            selected = viewModel.sessionMinutes == minutes,
                            onClick = { viewModel.sessionMinutes = minutes },
                            label = { Text(formatMinutes(minutes)) },
                        )
                    }
                }
                Text("Starts as soon as you save.", style = MaterialTheme.typography.bodyMedium)
            }
            RuleType.SCHEDULE -> {
                Text("Days", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = LockPolicy.isDayOn(viewModel.daysMask, day),
                            onClick = { viewModel.toggleDay(day) },
                            label = { Text(dayLabel(day)) },
                        )
                    }
                }
                Text("Time", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { editingStart = true }) {
                        Text(formatMinuteOfDay(viewModel.startMinute))
                    }
                    Text("to")
                    OutlinedButton(onClick = { editingStart = false }) {
                        Text(formatMinuteOfDay(viewModel.endMinute))
                    }
                }
                when {
                    viewModel.endMinute < viewModel.startMinute ->
                        Text("Runs overnight into the next day.", style = MaterialTheme.typography.bodyMedium)
                    viewModel.endMinute == viewModel.startMinute ->
                        Text("Locks for a full 24 hours.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Apps",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (blockedApps.isNotEmpty()) {
                val allSelected = blockedApps.all { it.packageName in viewModel.selectedPackages }
                TextButton(onClick = { viewModel.setAllPackages(!allSelected) }) {
                    Text(if (allSelected) "Select none" else "Select all")
                }
            }
        }
        if (blockedApps.isEmpty()) {
            Text("Add apps on the Apps tab first.", style = MaterialTheme.typography.bodyMedium)
        }
        blockedApps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.togglePackage(app.packageName) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(app.packageName, size = 32.dp)
                Text(app.label, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = app.packageName in viewModel.selectedPackages,
                    onCheckedChange = { viewModel.togglePackage(app.packageName) },
                )
            }
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
    }

    editingStart?.let { isStart ->
        val initial = if (isStart) viewModel.startMinute else viewModel.endMinute
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60)
        AlertDialog(
            onDismissRequest = { editingStart = null },
            title = { Text(if (isStart) "Start time" else "End time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val minute = state.hour * 60 + state.minute
                    if (isStart) viewModel.startMinute = minute else viewModel.endMinute = minute
                    editingStart = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editingStart = null }) { Text("Cancel") }
            },
        )
    }
}
