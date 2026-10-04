package com.example.btrapp.blocking.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.R
import com.example.btrapp.blocking.data.BlockedApp
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.domain.LockStatus
import com.example.btrapp.blocking.service.AppBlockerAccessibilityService
import com.example.btrapp.blocking.ui.common.AppIcon
import com.example.btrapp.blocking.ui.common.containerFactory
import com.example.btrapp.blocking.ui.common.formatMinutes
import com.example.btrapp.blocking.ui.common.formatTime
import com.example.btrapp.blocking.ui.common.isDebuggable
import com.example.btrapp.blocking.ui.common.rememberNow

private val lockNowOptions = listOf(15, 30, 60, 120, 240)

@Composable
fun HomeScreen(
    onManageApps: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = containerFactory { c, _ -> HomeViewModel(c.blockingRepository) }
    ),
) {
    val context = LocalContext.current
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val now = rememberNow(snapshot)

    var serviceEnabled by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        serviceEnabled = AppBlockerAccessibilityService.isEnabled(context)
        onPauseOrDispose { }
    }
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    var showLockAll by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val options = remember {
        if (context.isDebuggable()) listOf(1) + lockNowOptions else lockNowOptions
    }

    val statuses = blockedApps.associate {
        it.packageName to LockPolicy.status(it.packageName, now, snapshot)
    }
    val lockedCount = statuses.values.count { it is LockStatus.Locked }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!serviceEnabled) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Blocking is off", style = MaterialTheme.typography.titleMedium)
                        Text("BTR needs its accessibility service to see when a locked app opens.")
                        Button(onClick = { showDisclosure = true }) { Text("Turn on") }
                    }
                }
            }
        }

        item { StatusHeader(lockedCount = lockedCount, total = blockedApps.size) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (blockedApps.isNotEmpty()) {
                    TextButton(onClick = { showLockAll = true }) {
                        Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lock all")
                    }
                }
            }
        }

        if (blockedApps.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You haven't picked any apps to block yet.")
                    Button(onClick = onManageApps) { Text("Choose apps") }
                }
            }
        }

        items(blockedApps, key = { it.packageName }) { app ->
            val isExpanded = app.packageName in expanded
            AppLockCard(
                app = app,
                status = statuses[app.packageName] ?: LockStatus.Unlocked,
                expanded = isExpanded,
                options = options,
                onToggle = {
                    expanded = if (isExpanded) expanded - app.packageName else expanded + app.packageName
                },
                onLock = { minutes ->
                    viewModel.lockApps(minutes, listOf(app.packageName))
                    expanded = expanded - app.packageName
                },
            )
        }
    }

    if (showLockAll) {
        LockAllDialog(
            appCount = blockedApps.size,
            options = options,
            onDismiss = { showLockAll = false },
            onConfirm = { minutes ->
                viewModel.lockApps(minutes, blockedApps.map { it.packageName })
                showLockAll = false
            },
        )
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Allow BTR to block apps") },
            text = { Text(stringResource(R.string.accessibility_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    context.startActivity(AppBlockerAccessibilityService.settingsIntent())
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Not now") }
            },
        )
    }
}

@Composable
private fun StatusHeader(lockedCount: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$lockedCount of $total",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        total == 0 -> "Add apps to start building discipline."
                        lockedCount == 0 -> "Nothing locked right now. Tap an app to lock it."
                        lockedCount == total -> "Everything's locked. Stay strong."
                        else -> if (lockedCount == 1) "app locked" else "apps locked"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppLockCard(
    app: BlockedApp,
    status: LockStatus,
    expanded: Boolean,
    options: List<Int>,
    onToggle: () -> Unit,
    onLock: (minutes: Int) -> Unit,
) {
    var minutes by rememberSaveable(app.packageName) { mutableIntStateOf(60) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    ElevatedCard(onClick = onToggle, shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIcon(app.packageName, size = 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                StatusPill(status)
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider()
                Text("Lock for", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = minutes == option,
                            onClick = { minutes = option },
                            label = { Text(formatMinutes(option)) },
                        )
                    }
                }
                if (status is LockStatus.Locked) {
                    Text(
                        "Locked until ${formatTime(status.until)}. Locking again extends it if the new time is later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { onLock(minutes) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lock for ${formatMinutes(minutes)}")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: LockStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, container, content) = when (status) {
        is LockStatus.Locked ->
            Triple("Locked until ${formatTime(status.until)}", colors.primary, colors.onPrimary)
        is LockStatus.GrantActive ->
            Triple("Unlocked until ${formatTime(status.until)}", colors.tertiaryContainer, colors.onTertiaryContainer)
        LockStatus.Unlocked ->
            Triple("Not locked", colors.surfaceVariant, colors.onSurfaceVariant)
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (status is LockStatus.Locked) {
                Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(12.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LockAllDialog(
    appCount: Int,
    options: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(if (appCount == 1) "Lock 1 app" else "Lock all $appCount apps") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = minutes == option,
                        onClick = { minutes = option },
                        label = { Text(formatMinutes(option)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }) { Text("Lock for ${formatMinutes(minutes)}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
