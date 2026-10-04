package com.example.btrapp.blocking.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.btrapp.appContainer
import com.example.btrapp.blocking.data.BlockingRepository
import com.example.btrapp.blocking.domain.LockPolicy
import com.example.btrapp.blocking.domain.LockStatus
import com.example.btrapp.blocking.ui.block.BlockActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Watches which app is in the foreground and covers it with [BlockActivity] while it's locked.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val scope = MainScope()
    private lateinit var repository: BlockingRepository

    /** Overlays (notification shade, keyboards) that don't change which app the user is in. */
    private var transientPackages: Set<String> = emptySet()

    /** Foreground apps that are never blocked: BTR itself and launchers. */
    private var neutralPackages: Set<String> = emptySet()

    private var foregroundPackage: String? = null
    private var recheckJob: Job? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = appContainer.blockingRepository
        transientPackages = buildSet {
            add("com.android.systemui")
            getSystemService(InputMethodManager::class.java)
                ?.inputMethodList
                ?.forEach { add(it.packageName) }
        }
        neutralPackages = appContainer.installedApps.launcherPackages() + packageName
        // Rule changes (e.g. "Lock now") should take effect on the app that's already open.
        scope.launch {
            repository.snapshot.collect { foregroundPackage?.let(::evaluate) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in transientPackages) return
        if (pkg in neutralPackages) {
            foregroundPackage = null
            recheckJob?.cancel()
            return
        }
        foregroundPackage = pkg
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        val now = System.currentTimeMillis()
        val snapshot = repository.snapshot.value
        when (val status = LockPolicy.status(pkg, now, snapshot)) {
            is LockStatus.Locked -> {
                recheckJob?.cancel()
                showBlockScreen(pkg, now)
            }
            // Re-check exactly when the emergency grant runs out.
            is LockStatus.GrantActive -> scheduleRecheck(pkg, status.until - now)
            // A schedule may start while the app is open, so keep polling covered apps.
            LockStatus.Unlocked ->
                if (pkg in snapshot.coveredPackages) scheduleRecheck(pkg, IDLE_RECHECK_MS)
                else recheckJob?.cancel()
        }
    }

    private fun scheduleRecheck(pkg: String, delayMs: Long) {
        recheckJob?.cancel()
        recheckJob = scope.launch {
            delay(delayMs.coerceIn(500, IDLE_RECHECK_MS))
            if (foregroundPackage == pkg) evaluate(pkg)
        }
    }

    private fun showBlockScreen(pkg: String, now: Long) {
        // Several window events fire per app launch; one block screen is enough.
        if (pkg == lastBlockedPackage && now - lastBlockedAt < 1_000) return
        lastBlockedPackage = pkg
        lastBlockedAt = now
        startActivity(BlockActivity.intent(this, pkg))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val IDLE_RECHECK_MS = 30_000L

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val me = ComponentName(context, AppBlockerAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
