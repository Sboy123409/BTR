package com.example.btrapp.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.avatar.Avatar
import com.example.btrapp.avatar.Mood
import com.example.btrapp.blocking.ui.lock.LockSummaryCard
import com.example.btrapp.core.ui.rememberNow
import com.example.btrapp.habits.ui.TodayChecklistCard
import java.time.Instant
import java.time.ZoneId

/** Home: the avatar, today's checklist and App Lock at a glance. */
@Composable
fun HubScreen(
    onEditAvatar: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenLock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HubViewModel = viewModel(factory = HubViewModel.Factory),
) {
    val avatar by viewModel.avatar.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val now = rememberNow(periodMs = 60_000)
    val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    val done = today?.done ?: 0
    val total = today?.total ?: 0
    val mood = Mood.from(done, total, hour)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Avatar(
                    config = avatar,
                    mood = mood,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Edit avatar", onClick = onEditAvatar),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (avatar.name.isBlank()) "Hi there" else "Hi, ${avatar.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        greeting(mood, done, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditAvatar) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit avatar")
                }
            }
        }
        item { TodayChecklistCard(onOpenHabits = onOpenHabits) }
        item { LockSummaryCard(onOpenLock = onOpenLock) }
    }
}

private fun greeting(mood: Mood, done: Int, total: Int): String = when {
    total == 0 -> "Plan today and I'll cheer you on."
    mood == Mood.Ecstatic -> "All $total done. Legend."
    mood == Mood.Happy -> "$done of $total done. Keep rolling."
    mood == Mood.Sleepy -> "Still $total to go. One small one before bed?"
    else -> "$total on the list. Let's start one."
}
