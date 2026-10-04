package com.example.btrapp.blocking.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.domain.InstalledApp
import com.example.btrapp.blocking.domain.InstalledAppsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [blocked] is what's saved; [checked] includes the user's unconfirmed change. */
data class AppRow(val app: InstalledApp, val blocked: Boolean, val checked: Boolean) {
    val pending: Boolean get() = blocked != checked
}

data class AppsUiState(
    val loading: Boolean = true,
    val rows: List<AppRow> = emptyList(),
    val toAdd: List<InstalledApp> = emptyList(),
    val toRemove: List<InstalledApp> = emptyList(),
) {
    val pendingCount: Int get() = toAdd.size + toRemove.size
}

class AppsViewModel(
    private val repository: BlockingRepository,
    installedAppsProvider: InstalledAppsProvider,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>?>(null)
    val query = MutableStateFlow("")

    /** Packages whose checkbox differs from what's saved, awaiting confirmation. */
    private val toggled = MutableStateFlow(emptySet<String>())

    init {
        viewModelScope.launch { installedApps.value = installedAppsProvider.launchableApps() }
    }

    val state: StateFlow<AppsUiState> =
        combine(installedApps, repository.blockedApps, query, toggled) { apps, blocked, q, toggled ->
            if (apps == null) return@combine AppsUiState(loading = true)
            val blockedSet = blocked.mapTo(HashSet()) { it.packageName }
            val all = apps.map {
                val isBlocked = it.packageName in blockedSet
                AppRow(it, blocked = isBlocked, checked = isBlocked != (it.packageName in toggled))
            }
            val rows = all
                .filter { q.isBlank() || it.app.label.contains(q.trim(), ignoreCase = true) }
                // Blocked apps first, then alphabetical (already sorted by the provider).
                // Sorted by saved state so rows don't jump while picking.
                .sortedByDescending { it.blocked }
            AppsUiState(
                loading = false,
                rows = rows,
                toAdd = all.filter { it.pending && it.checked }.map { it.app },
                toRemove = all.filter { it.pending && !it.checked }.map { it.app },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun toggle(app: InstalledApp) {
        toggled.value =
            if (app.packageName in toggled.value) toggled.value - app.packageName
            else toggled.value + app.packageName
    }

    fun discardChanges() {
        toggled.value = emptySet()
    }

    fun confirmChanges() {
        viewModelScope.launch {
            val s = state.first { !it.loading }
            s.toAdd.forEach { repository.setBlocked(it.packageName, it.label, true) }
            s.toRemove.forEach { repository.setBlocked(it.packageName, it.label, false) }
            toggled.value = emptySet()
        }
    }
}
