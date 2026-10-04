package com.example.btrapp.blocking.ui.lock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.btrapp.blocking.ui.history.HistoryScreen
import com.example.btrapp.blocking.ui.rules.RulesScreen

private val pages = listOf("Locks", "Rules", "History")

/** The Lock tab: per-app locks, schedules and the emergency-unlock history. */
@Composable
internal fun LockScreen(
    onManageApps: () -> Unit,
    onEditRule: (Long) -> Unit,
    onNewRule: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = page) {
            pages.forEachIndexed { index, title ->
                Tab(selected = page == index, onClick = { page = index }, text = { Text(title) })
            }
        }
        val content = Modifier.weight(1f)
        when (page) {
            0 -> LocksPage(onManageApps = onManageApps, modifier = content)
            1 -> RulesScreen(
                onEditRule = onEditRule,
                onNewRule = onNewRule,
                onOpenSessions = onOpenSessions,
                modifier = content,
            )
            else -> HistoryScreen(modifier = content)
        }
    }
}
