package com.example.btrapp.blocking.web

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The web filter is on until turned off, and turning it off only takes effect after
 * [COOLDOWN_MS], so the urge has time to pass. The cooldown can be cancelled.
 */
data class WebFilterState(val enabled: Boolean, val disableAt: Long?) {
    fun isActive(now: Long): Boolean = enabled && (disableAt == null || now < disableAt)
}

class WebFilterSettings(context: Context, private val clock: () -> Long = System::currentTimeMillis) {

    private val prefs = context.getSharedPreferences("web_filter", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<WebFilterState> = _state.asStateFlow()

    fun enable() = write(WebFilterState(enabled = true, disableAt = null))

    fun requestDisable() {
        if (_state.value.disableAt == null) write(_state.value.copy(disableAt = clock() + COOLDOWN_MS))
    }

    fun cancelDisable() = write(_state.value.copy(disableAt = null))

    /** Turns the filter fully off once its cooldown has run out. Returns whether it should run. */
    fun settle(): Boolean {
        val current = _state.value
        if (current.enabled && !current.isActive(clock())) write(WebFilterState(false, null))
        return _state.value.enabled
    }

    private fun read() = WebFilterState(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        disableAt = prefs.getLong(KEY_DISABLE_AT, 0L).takeIf { it > 0 },
    )

    private fun write(state: WebFilterState) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, state.enabled)
            .putLong(KEY_DISABLE_AT, state.disableAt ?: 0L)
            .apply()
        _state.value = state
    }

    companion object {
        const val COOLDOWN_MS = 60 * 60_000L
        private const val KEY_ENABLED = "enabled"
        private const val KEY_DISABLE_AT = "disable_at"
    }
}
