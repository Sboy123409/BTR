package com.example.btrapp.blocking.ui.block

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.btrapp.appContainer
import com.example.btrapp.blocking.domain.Exercise
import com.example.btrapp.blocking.domain.ExerciseCatalog
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.domain.LockStatus
import com.example.btrapp.blocking.ui.common.AppIcon
import com.example.btrapp.blocking.ui.common.formatTime
import com.example.btrapp.blocking.ui.common.isDebuggable
import com.example.btrapp.blocking.ui.common.rememberNow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_REASON_LENGTH = 20

private enum class Step { LOCKED, REASON, DURATION, EXERCISE, WORKOUT }

@Composable
fun BlockScreen(
    packageName: String,
    onGoHome: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val repository = container.blockingRepository
    val label = remember(packageName) { container.installedApps.label(packageName) }
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val now = rememberNow(snapshot, periodMs = 5_000)
    val status = LockPolicy.status(packageName, now, snapshot)

    // A different locked app restarts the flow from the top.
    key(packageName) {
        var step by rememberSaveable { mutableStateOf(Step.LOCKED) }
        var reason by rememberSaveable { mutableStateOf("") }
        var durationMin by rememberSaveable { mutableIntStateOf(0) }
        var exercise by rememberSaveable { mutableStateOf<Exercise?>(null) }
        val priorUnlocks by produceState(0) { value = repository.unlocksToday() }

        BackHandler {
            step = when (step) {
                Step.LOCKED -> return@BackHandler onGoHome()
                Step.REASON -> Step.LOCKED
                Step.DURATION -> Step.REASON
                Step.EXERCISE -> Step.DURATION
                Step.WORKOUT -> Step.EXERCISE
            }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon(packageName, size = 64.dp)
                Text(label, style = MaterialTheme.typography.headlineSmall)

                when (step) {
                    Step.LOCKED -> LockedStep(
                        status = status,
                        label = label,
                        onGoHome = onGoHome,
                        onEmergency = { step = Step.REASON },
                        onOpen = onUnlocked,
                    )
                    Step.REASON -> ReasonStep(
                        reason = reason,
                        onReasonChange = { reason = it },
                        onNext = { step = Step.DURATION },
                        onCancel = onGoHome,
                    )
                    Step.DURATION -> DurationStep(
                        priorUnlocks = priorUnlocks,
                        onPick = {
                            durationMin = it
                            step = Step.EXERCISE
                        },
                        onCancel = onGoHome,
                    )
                    Step.EXERCISE -> ExerciseStep(
                        durationMin = durationMin,
                        priorUnlocks = priorUnlocks,
                        onPick = {
                            exercise = it
                            step = Step.WORKOUT
                        },
                        onCancel = onGoHome,
                    )
                    Step.WORKOUT -> {
                        val picked = exercise ?: return@Column
                        val amount = ExerciseCatalog.amount(picked, durationMin, priorUnlocks)
                        val scope = rememberCoroutineScope()
                        var saving by remember { mutableStateOf(false) }
                        WorkoutStep(
                            exercise = picked,
                            amount = amount,
                            saving = saving,
                            onDone = {
                                saving = true
                                scope.launch {
                                    repository.recordEmergencyUnlock(
                                        packageName = packageName,
                                        reason = reason.trim(),
                                        exercise = picked.name,
                                        amount = amount,
                                        durationMin = durationMin,
                                    )
                                    onUnlocked()
                                }
                            },
                            onCancel = onGoHome,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LockedStep(
    status: LockStatus,
    label: String,
    onGoHome: () -> Unit,
    onEmergency: () -> Unit,
    onOpen: () -> Unit,
) {
    if (status !is LockStatus.Locked) {
        Text("$label isn't locked right now.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Open $label") }
        return
    }
    Text(
        "Locked until ${formatTime(status.until)}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        "You decided to keep this app closed. Stick with it.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) { Text("Go home") }
    TextButton(onClick = onEmergency) { Text("Emergency unlock") }
}

@Composable
private fun ReasonStep(
    reason: String,
    onReasonChange: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val length = reason.trim().length
    Text("Why do you need it right now?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Be honest. This goes in your unlock history.",
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
        value = reason,
        onValueChange = onReasonChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        placeholder = { Text("I need to reply to my manager about…") },
        supportingText = {
            Text(
                if (length < MIN_REASON_LENGTH) "$length / $MIN_REASON_LENGTH characters minimum"
                else "$length characters"
            )
        },
    )
    Button(
        onClick = onNext,
        enabled = length >= MIN_REASON_LENGTH,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Next") }
    TextButton(onClick = onCancel) { Text("Never mind, go home") }
}

@Composable
private fun DurationStep(priorUnlocks: Int, onPick: (Int) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val durations = remember {
        if (context.isDebuggable()) listOf(1) + ExerciseCatalog.unlockDurations
        else ExerciseCatalog.unlockDurations
    }
    Text("How long do you need?", style = MaterialTheme.typography.titleLarge)
    Text(
        "Longer unlocks mean a harder punishment.",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (priorUnlocks > 0) {
        EscalationNote(priorUnlocks)
    }
    durations.forEach { minutes ->
        OutlinedButton(onClick = { onPick(minutes) }, modifier = Modifier.fillMaxWidth()) {
            Text("$minutes min  ·  ×${ExerciseCatalog.durationMultiplier(minutes)} difficulty")
        }
    }
    TextButton(onClick = onCancel) { Text("Never mind, go home") }
}

@Composable
private fun ExerciseStep(
    durationMin: Int,
    priorUnlocks: Int,
    onPick: (Exercise) -> Unit,
    onCancel: () -> Unit,
) {
    Text("Choose your punishment", style = MaterialTheme.typography.titleLarge)
    if (priorUnlocks > 0) {
        EscalationNote(priorUnlocks)
    }
    Exercise.entries.forEach { exercise ->
        val amount = ExerciseCatalog.amount(exercise, durationMin, priorUnlocks)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(exercise) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(exercise.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    ExerciseCatalog.describe(exercise, amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    TextButton(onClick = onCancel) { Text("Never mind, go home") }
}

@Composable
private fun WorkoutStep(
    exercise: Exercise,
    amount: Int,
    saving: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val total = ExerciseCatalog.minimumSeconds(exercise, amount)
    var remaining by rememberSaveable(exercise, amount) { mutableIntStateOf(total) }
    LaunchedEffect(exercise, amount) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }

    Text(
        ExerciseCatalog.describe(exercise, amount).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(exercise.instructions, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { 1f - remaining / total.toFloat() },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        if (remaining > 0) "Keep going… ${remaining}s" else "Done? Be honest.",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(
        onClick = onDone,
        enabled = remaining == 0 && !saving,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("I did it, unlock") }
    TextButton(onClick = onCancel) { Text("Give up, go home") }
}

@Composable
private fun EscalationNote(priorUnlocks: Int) {
    val percent = (ExerciseCatalog.ESCALATION_PER_UNLOCK * 100 * priorUnlocks).toInt()
    Text(
        "You've used $priorUnlocks emergency unlock${if (priorUnlocks == 1) "" else "s"} today, " +
            "so every punishment is +$percent%.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
