package com.example.btrapp.blocking.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.btrapp.ui.theme.PosterBlack
import com.example.btrapp.ui.theme.PosterCream
import com.example.btrapp.ui.theme.PosterDeepRed
import com.example.btrapp.ui.theme.PosterRed

private val lockNowOptions = listOf(15, 30, 60, 120, 240)

private val GoDarkGradient = Brush.linearGradient(listOf(PosterRed, Color(0xFF7A0E12), PosterDeepRed))

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
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var removing by remember { mutableStateOf<BlockedApp?>(null) }
    val options = remember {
        if (context.isDebuggable()) listOf(1) + lockNowOptions else lockNowOptions
    }

    val statuses = blockedApps.associate {
        it.packageName to LockPolicy.status(it.packageName, now, snapshot)
    }
    val lockedUntils = statuses.values.filterIsInstance<LockStatus.Locked>().map { it.until }

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

        if (blockedApps.isNotEmpty()) {
            item {
                StatusBanner(
                    lockedCount = lockedUntils.size,
                    total = blockedApps.size,
                    nextUnlock = lockedUntils.minOrNull(),
                )
            }
            item {
                GoDarkCard(
                    appCount = blockedApps.size,
                    allLockedUntil = if (lockedUntils.size == blockedApps.size) lockedUntils.minOrNull() else null,
                    options = options,
                    onLockAll = { minutes -> viewModel.lockApps(minutes, blockedApps.map { it.packageName }) },
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your apps", style = MaterialTheme.typography.titleMedium)
                if (blockedApps.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            "${blockedApps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onManageApps) {
                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add apps")
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
                onRemove = { removing = app },
            )
        }
    }

    removing?.let { app ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${app.label}?") },
            text = { Text("It'll also be taken out of any lock rules.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unblock(app)
                    expanded = expanded - app.packageName
                    removing = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
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

/** Slim notification-style strip summarising what's locked. */
@Composable
private fun StatusBanner(lockedCount: Int, total: Int, nextUnlock: Long?) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(16.dp))
            Text(
                if (nextUnlock == null) "Nothing locked. Stay sharp."
                else "$lockedCount of $total locked · next unlock ${formatTime(nextUnlock)}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** The emphasised "lock everything" action, in the poster's colours. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoDarkCard(
    appCount: Int,
    allLockedUntil: Long?,
    options: List<Int>,
    onLockAll: (minutes: Int) -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(60) }
    Box(
        Modifier
            .fillMaxWidth()
            .background(GoDarkGradient, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    "GO DARK",
                    color = PosterCream,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                )
                Text(
                    if (allLockedUntil != null) "Everything's locked until ${formatTime(allLockedUntil)}"
                    else if (appCount == 1) "Lock your app now" else "Lock all $appCount apps at once",
                    color = PosterCream.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = minutes == option,
                        onClick = { minutes = option },
                        label = { Text(formatMinutes(option)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = PosterCream,
                            selectedContainerColor = PosterCream,
                            selectedLabelColor = PosterBlack,
                        ),
                        border = BorderStroke(1.dp, PosterCream.copy(alpha = 0.7f)),
                    )
                }
            }
            Button(
                onClick = { onLockAll(minutes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PosterCream,
                    contentColor = PosterBlack,
                ),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (allLockedUntil != null) "Extend all: ${formatMinutes(minutes)} from now"
                    else "Lock all for ${formatMinutes(minutes)}",
                    fontWeight = FontWeight.Bold,
                )
            }
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
    onRemove: () -> Unit,
) {
    var minutes by rememberSaveable(app.packageName) { mutableIntStateOf(60) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    // An emergency unlock is temporary, so the app still counts as locked for removal.
    val locked = status !is LockStatus.Unlocked

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
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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
                // Removing a locked app would be an easy way around the lock.
                TextButton(
                    onClick = onRemove,
                    enabled = !locked,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(if (locked) "Can't remove while locked" else "Remove from block list")
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
