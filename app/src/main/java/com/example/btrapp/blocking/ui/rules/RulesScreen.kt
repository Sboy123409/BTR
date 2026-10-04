package com.example.btrapp.blocking.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.blocking.data.LockRule
import com.example.btrapp.blocking.data.RuleType
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.ui.common.containerFactory
import com.example.btrapp.blocking.ui.common.formatDays
import com.example.btrapp.blocking.ui.common.formatMinuteOfDay
import com.example.btrapp.blocking.ui.common.formatTime
import com.example.btrapp.blocking.ui.common.rememberNow

@Composable
fun RulesScreen(
    onEditRule: (Long) -> Unit,
    onNewRule: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = viewModel(
        factory = containerFactory { c, _ -> RulesViewModel(c.blockingRepository) }
    ),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val labels = blockedApps.associate { it.packageName to it.label }
    val now = rememberNow(rules)

    Box(modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            Text(
                "No lock rules yet. Add a schedule like \"Weekdays 9–5\", or use Lock now on Home.",
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rules, key = { it.rule.id }) { item ->
                val rule = item.rule
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRule(rule.id) },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ruleTitle(rule, now), style = MaterialTheme.typography.titleMedium)
                            val active = rule.enabled && LockPolicy.activeUntil(rule, now) != null
                            if (active) {
                                Text(
                                    "Active now",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                item.packageNames.joinToString { labels[it] ?: it },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { viewModel.setEnabled(rule.id, it) },
                        )
                        IconButton(onClick = { viewModel.delete(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewRule,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New rule") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

private fun ruleTitle(rule: LockRule, now: Long): String = when (rule.type) {
    RuleType.SESSION -> {
        val end = rule.endAt ?: 0
        if (end > now) "Session until ${formatTime(end)}" else "Session ended"
    }
    RuleType.SCHEDULE ->
        "${formatDays(rule.daysMask ?: 0)} · " +
            "${formatMinuteOfDay(rule.startMinute ?: 0)}–${formatMinuteOfDay(rule.endMinute ?: 0)}"
}
