package com.example.btrapp.blocking.ui.lock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.btrapp.blocking.appLockGraph
import com.example.btrapp.blocking.web.WebFilterSettings
import com.example.btrapp.blocking.web.WebFilterVpnService
import com.example.btrapp.core.ui.formatTime
import com.example.btrapp.core.ui.rememberNow

/** Turns the adult-site filter on, and makes turning it off wait out a cooldown. */
@Composable
internal fun WebFilterCard() {
    val context = LocalContext.current
    val settings = context.appLockGraph.webFilterSettings
    val state by settings.state.collectAsStateWithLifecycle()
    val running by WebFilterVpnService.running.collectAsStateWithLifecycle()
    val now = rememberNow(state)
    var confirmingOff by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    fun activate() {
        settings.enable()
        WebFilterVpnService.start(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // So the user hears about it if the filter gets switched off from outside BTR.
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val vpnConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == Activity.RESULT_OK) activate() }

    fun turnOn() {
        WebFilterVpnService.consentIntent(context)?.let(vpnConsent::launch) ?: activate()
    }

    val disableAt = state.disableAt
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Web filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        !state.isActive(now) -> "Off"
                        running -> "On"
                        else -> "Paused"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.isActive(now) && running) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                !state.enabled -> {
                    Text(
                        "Blocks adult websites in every browser and app. Lookups are checked on " +
                            "your phone; your browsing isn't sent to BTR.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = ::turnOn, modifier = Modifier.fillMaxWidth()) { Text("Turn on") }
                }
                !running -> {
                    Text(
                        "The filter was switched off outside BTR, so adult sites aren't blocked right now.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = ::turnOn, modifier = Modifier.fillMaxWidth()) { Text("Turn back on") }
                }
                disableAt != null -> {
                    Text(
                        "Turning off at ${formatTime(disableAt)}. Changed your mind?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = settings::cancelDisable, modifier = Modifier.fillMaxWidth()) {
                        Text("Keep it on")
                    }
                }
                else -> {
                    Text("Adult sites are blocked.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Make it stick: turn on Always-on VPN") }
                    TextButton(onClick = { confirmingOff = true }) { Text("Turn off") }
                }
            }

            if (state.isActive(now) && WebFilterVpnService.privateDnsBypasses(context)) {
                Text(
                    "Private DNS is set to a custom provider in Settings, which skips this filter. " +
                        "Set it to Automatic or Off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (confirmingOff) {
        val minutes = WebFilterSettings.COOLDOWN_MS / 60_000
        AlertDialog(
            onDismissRequest = { confirmingOff = false },
            title = { Text("Turn off the web filter?") },
            text = {
                Text(
                    "It stays on for another $minutes minutes, so the urge has time to pass. " +
                        "You can cancel any time before then."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.requestDisable()
                    confirmingOff = false
                }) { Text("Start $minutes-min countdown") }
            },
            dismissButton = { TextButton(onClick = { confirmingOff = false }) { Text("Keep it on") } },
        )
    }
}
