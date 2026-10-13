package com.example.btrapp.blocking.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.example.btrapp.blocking.appLockGraph
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
    private var pipRecheckJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = appLockGraph.blockingRepository
        transientPackages = buildSet {
            add("com.android.systemui")
            getSystemService(InputMethodManager::class.java)
                ?.inputMethodList
                ?.forEach { add(it.packageName) }
        }
        neutralPackages = appLockGraph.installedApps.launcherPackages() + packageName
        // Rule changes (e.g. "Lock now") should take effect on the app that's already open.
        scope.launch {
            repository.snapshot.collect {
                foregroundPackage?.let(::evaluate)
                checkPictureInPicture()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowStateChanged(event)
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> checkPictureInPicture()
        }
    }

    private fun onWindowStateChanged(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg in transientPackages) return
        // Our block screen is up, so returning to the locked app (e.g. from Recents) must block again.
        if (pkg == packageName) lastBlockedPackage = null
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
            // A schedule may start while the app is open, so wake up when the next one does.
            LockStatus.Unlocked ->
                LockPolicy.nextLockStart(pkg, now, snapshot)
                    ?.let { scheduleRecheck(pkg, it - now) }
                    ?: recheckJob?.cancel()
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
        // Send the locked app to the background first, so nothing usable is left under the block screen.
        performGlobalAction(GLOBAL_ACTION_HOME)
        scope.launch {
            delay(HOME_SETTLE_MS)
            startActivity(BlockActivity.intent(this@AppBlockerAccessibilityService, pkg))
        }
    }

    /** Picture-in-picture windows float over every app (even the launcher), so close locked ones directly. */
    private fun checkPictureInPicture() {
        if (!::repository.isInitialized) return
        val now = System.currentTimeMillis()
        val snapshot = repository.snapshot.value
        var nextCheck: Long? = null
        windows.filter { it.isInPictureInPictureMode }.forEach { window ->
            val root = window.root ?: return@forEach
            val pkg = root.packageName?.toString() ?: return@forEach
            when (val status = LockPolicy.status(pkg, now, snapshot)) {
                is LockStatus.Locked -> root.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                is LockStatus.GrantActive -> nextCheck = minOf(nextCheck ?: status.until, status.until)
                LockStatus.Unlocked -> LockPolicy.nextLockStart(pkg, now, snapshot)
                    ?.let { nextCheck = minOf(nextCheck ?: it, it) }
            }
        }
        pipRecheckJob?.cancel()
        nextCheck?.let { at ->
            pipRecheckJob = scope.launch {
                delay((at - now).coerceIn(500, IDLE_RECHECK_MS))
                checkPictureInPicture()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val IDLE_RECHECK_MS = 30_000L
        private const val HOME_SETTLE_MS = 250L

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
