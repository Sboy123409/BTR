package com.example.btrapp.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.btrapp.blocking.ui.lock.LockSummaryCard
import com.example.btrapp.habits.ui.TodayChecklistCard

/** Home: a summary of every module. */
@Composable
fun HubScreen(
    onOpenHabits: () -> Unit,
    onOpenLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TodayChecklistCard(onOpenHabits = onOpenHabits) }
        item { LockSummaryCard(onOpenLock = onOpenLock) }
    }
}
