package com.example.btrapp.blocking.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.blocking.appLockGraph
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.domain.LockStatus
import com.example.btrapp.blocking.ui.common.containerFactory
import com.example.btrapp.blocking.web.WebFilterVpnService
import com.example.btrapp.core.ui.rememberNow

/** App Lock at a glance for the Home hub: the blocking-off warning, status strip and Go Dark. */
@Composable
fun LockSummaryCard(
    onOpenLock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = viewModel(
        factory = containerFactory { c, _ -> LockViewModel(c.blockingRepository) }
    ),
) {
    val context = LocalContext.current
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val now = rememberNow(snapshot)
    val options = remember { lockOptions(context) }
    val lockedUntils = blockedApps
        .map { LockPolicy.status(it.packageName, now, snapshot) }
        .filterIsInstance<LockStatus.Locked>()
        .map { it.until }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BlockingOffCard()
        if (blockedApps.isEmpty()) {
            OutlinedCard(onClick = onOpenLock, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("App Lock", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pick the apps that pull you in, then lock them on a schedule or on demand.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onOpenLock) { Text("Set up") }
                }
            }
        } else {
            StatusBanner(
                lockedCount = lockedUntils.size,
                total = blockedApps.size,
                nextUnlock = lockedUntils.minOrNull(),
            )
            GoDarkCard(
                appCount = blockedApps.size,
                allLockedUntil = if (lockedUntils.size == blockedApps.size) lockedUntils.minOrNull() else null,
                options = options,
                onLockAll = { minutes -> viewModel.lockApps(minutes, blockedApps.map { it.packageName }) },
            )
        }
        WebFilterStatusLine(onOpenLock)
    }
}

@Composable
private fun WebFilterStatusLine(onOpenLock: () -> Unit) {
    val webFilter by LocalContext.current.appLockGraph.webFilterSettings.state.collectAsStateWithLifecycle()
    val running by WebFilterVpnService.running.collectAsStateWithLifecycle()
    val now = rememberNow(webFilter)
    val (text, isProblem) = when {
        !webFilter.enabled -> "Web filter is off" to false
        !running -> "Web filter isn't running. Tap to turn it back on" to true
        webFilter.isActive(now) && webFilter.disableAt != null -> "Web filter turning off soon" to false
        else -> "Web filter on: adult sites blocked" to false
    }
    TextButton(onClick = onOpenLock) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
