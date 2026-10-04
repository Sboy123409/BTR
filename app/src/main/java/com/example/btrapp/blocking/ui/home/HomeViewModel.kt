package com.example.btrapp.blocking.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btrapp.blocking.data.BlockedApp
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.domain.LockSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: BlockingRepository) : ViewModel() {

    val blockedApps: StateFlow<List<BlockedApp>> = repository.blockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snapshot: StateFlow<LockSnapshot> = repository.snapshot

    /** Starts a session now. Locking an already-locked app extends it, since the latest end wins. */
    fun lockApps(minutes: Int, packages: Collection<String>) {
        if (packages.isEmpty()) return
        viewModelScope.launch { repository.startSession(minutes, packages) }
    }

    fun unblock(app: BlockedApp) {
        viewModelScope.launch { repository.setBlocked(app.packageName, app.label, false) }
    }
}
