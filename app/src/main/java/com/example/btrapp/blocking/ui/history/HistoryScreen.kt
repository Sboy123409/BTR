package com.example.btrapp.blocking.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.btrapp.appContainer
import com.example.btrapp.blocking.domain.Exercise
import com.example.btrapp.blocking.domain.ExerciseCatalog
import com.example.btrapp.blocking.ui.common.AppIcon
import com.example.btrapp.blocking.ui.common.formatDateTime
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val container = LocalContext.current.appContainer
    val unlocks by container.blockingRepository.unlockHistory
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val zone = ZoneId.systemDefault()
    val startOfToday = remember { LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli() }
    val startOfWeek = startOfToday - 6 * 24 * 60 * 60 * 1000L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Emergency unlocks: ${unlocks.count { it.createdAt >= startOfToday }} today · " +
                    "${unlocks.count { it.createdAt >= startOfWeek }} in the last 7 days",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (unlocks.isEmpty()) {
            item { Text("No emergency unlocks yet. Keep it that way.") }
        }
        items(unlocks, key = { it.id }) { unlock ->
            val label = remember(unlock.packageName) {
                container.installedApps.label(unlock.packageName)
            }
            val exercise = Exercise.entries.firstOrNull { it.name == unlock.exercise }
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppIcon(unlock.packageName, size = 24.dp)
                        Text(label, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        formatDateTime(unlock.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("“${unlock.reason}”", fontStyle = FontStyle.Italic)
                    Text(
                        (exercise?.let { ExerciseCatalog.describe(it, unlock.amount) }
                            ?: "${unlock.amount} ${unlock.exercise}") +
                            " for ${unlock.durationMin} min",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
