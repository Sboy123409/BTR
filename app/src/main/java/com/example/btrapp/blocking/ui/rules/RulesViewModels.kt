package com.example.btrapp.blocking.ui.rules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btrapp.blocking.data.BlockedApp
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.data.LockRule
import com.example.btrapp.blocking.data.RuleType
import com.example.btrapp.blocking.data.RuleWithApps
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(private val repository: BlockingRepository) : ViewModel() {

    val rules: StateFlow<List<RuleWithApps>> = repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blockedApps: StateFlow<List<BlockedApp>> = repository.blockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.deleteEndedSessions() }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setRuleEnabled(id, enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteRule(id) }
    }
}

class RuleEditorViewModel(
    private val repository: BlockingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ruleId: Long = savedStateHandle.get<Long>(ARG_RULE_ID) ?: NEW_RULE
    val isNew = ruleId == NEW_RULE

    var type by mutableStateOf(RuleType.SCHEDULE)
    var sessionMinutes by mutableIntStateOf(60)
    var daysMask by mutableIntStateOf(WEEKDAYS)
    var startMinute by mutableIntStateOf(9 * 60)
    var endMinute by mutableIntStateOf(17 * 60)
    var selectedPackages by mutableStateOf(emptySet<String>())
    private var enabled = true

    val blockedApps: StateFlow<List<BlockedApp>> = repository.blockedApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val existing = if (isNew) null else repository.getRule(ruleId)
            // New rules start empty so each rule covers only the apps picked for it.
            if (existing == null) return@launch
            val rule = existing.rule
            type = rule.type
            enabled = rule.enabled
            rule.daysMask?.let { daysMask = it }
            rule.startMinute?.let { startMinute = it }
            rule.endMinute?.let { endMinute = it }
            if (rule.startAt != null && rule.endAt != null) {
                sessionMinutes = ((rule.endAt - rule.startAt) / 60_000).toInt()
            }
            selectedPackages = existing.packageNames
        }
    }

    val validationError: String?
        get() = when {
            selectedPackages.isEmpty() -> "Pick at least one app"
            type == RuleType.SCHEDULE && daysMask == 0 -> "Pick at least one day"
            else -> null
        }

    fun toggleDay(isoDay: Int) {
        daysMask = daysMask xor (1 shl (isoDay - 1))
    }

    fun togglePackage(packageName: String) {
        selectedPackages =
            if (packageName in selectedPackages) selectedPackages - packageName
            else selectedPackages + packageName
    }

    fun setAllPackages(selected: Boolean) {
        selectedPackages =
            if (selected) blockedApps.value.mapTo(HashSet()) { it.packageName } else emptySet()
    }

    fun save(onSaved: () -> Unit) {
        if (validationError != null) return
        val id = if (isNew) 0L else ruleId
        val rule = when (type) {
            // Saving a session (re)starts it from now.
            RuleType.SESSION -> {
                val now = System.currentTimeMillis()
                LockRule(
                    id = id,
                    type = type,
                    enabled = enabled,
                    startAt = now,
                    endAt = now + sessionMinutes * 60_000L,
                )
            }
            RuleType.SCHEDULE -> LockRule(
                id = id,
                type = type,
                enabled = enabled,
                daysMask = daysMask,
                startMinute = startMinute,
                endMinute = endMinute,
            )
        }
        viewModelScope.launch {
            repository.saveRule(rule, selectedPackages)
            onSaved()
        }
    }

    companion object {
        const val ARG_RULE_ID = "ruleId"
        const val NEW_RULE = 0L
        private const val WEEKDAYS = 0b0011111
    }
}
