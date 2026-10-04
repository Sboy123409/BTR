package com.example.btrapp.blocking.ui.rules

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.blocking.data.SESSION_LOG_DAYS
import com.example.btrapp.blocking.ui.common.containerFactory
import com.example.btrapp.blocking.ui.common.formatDateTime
import com.example.btrapp.blocking.ui.common.formatMinutes
import com.example.btrapp.blocking.ui.common.formatTime
import com.example.btrapp.blocking.ui.common.rememberNow

/** Log of one-off sessions started from Home, kept for [SESSION_LOG_DAYS] days. */
@Composable
fun SessionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionsViewModel = viewModel(
        factory = containerFactory { c, _ -> SessionsViewModel(c.blockingRepository) }
    ),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val labels = blockedApps.associate { it.packageName to it.label }
    val now = rememberNow(sessions)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Session log", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Text(
                "Every Go Dark and Lock now from the last $SESSION_LOG_DAYS days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (sessions.isEmpty()) {
            item {
                Text("No sessions yet.", modifier = Modifier.padding(start = 8.dp))
            }
        }
        items(sessions, key = { it.rule.id }) { item ->
            val start = item.rule.startAt ?: 0
            val end = item.rule.endAt ?: 0
            val active = now in start until end
            Card(Modifier.fillMaxWidth().padding(start = 8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDateTime(start),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    "Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        "${formatMinutes(((end - start) / 60_000).toInt())} · " +
                            if (active) "until ${formatTime(end)}" else "ended ${formatTime(end)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        item.packageNames.joinToString { labels[it] ?: it }.ifEmpty { "No apps" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
