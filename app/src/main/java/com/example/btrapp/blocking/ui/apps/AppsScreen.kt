package com.example.btrapp.blocking.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btrapp.blocking.domain.InstalledApp
import com.example.btrapp.blocking.ui.common.AppIcon
import com.example.btrapp.blocking.ui.common.containerFactory

@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = viewModel(
        factory = containerFactory { c, _ -> AppsViewModel(c.blockingRepository, c.installedApps) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var reviewing by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search apps") },
            singleLine = true,
        )
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.rows, key = { it.app.packageName }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggle(row.app) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppIcon(row.app.packageName)
                    Column(Modifier.weight(1f)) {
                        Text(row.app.label, style = MaterialTheme.typography.bodyLarge)
                        if (row.pending) {
                            Text(
                                if (row.checked) "Will be added" else "Will be removed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Checkbox(
                        checked = row.checked,
                        onCheckedChange = { viewModel.toggle(row.app) },
                    )
                }
            }
        }
        if (state.pendingCount > 0) {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (state.pendingCount == 1) "1 change" else "${state.pendingCount} changes",
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::discardChanges) { Text("Discard") }
                    Button(onClick = { reviewing = true }) { Text("Review") }
                }
            }
        }
    }

    if (reviewing && state.pendingCount > 0) {
        AlertDialog(
            onDismissRequest = { reviewing = false },
            title = { Text("Update block list?") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChangeSection("Add to block list", state.toAdd)
                    ChangeSection("Remove from block list", state.toRemove)
                    if (state.toRemove.isNotEmpty()) {
                        Text(
                            "Removed apps are also taken out of any lock rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmChanges()
                    reviewing = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { reviewing = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun ChangeSection(title: String, apps: List<InstalledApp>) {
    if (apps.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall)
    apps.forEach { app ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app.packageName, size = 28.dp)
            Text(app.label)
        }
    }
}
