package com.example.btrapp.blocking.domain

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

class InstalledAppsProvider(context: Context) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    /** Apps that show up in the launcher, excluding BTR itself. */
    suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != appContext.packageName }
            .distinctBy { it.first }
            .map { (pkg, label) -> InstalledApp(pkg, label) }
            .sortedBy { it.label.lowercase() }
    }

    /** Home-screen apps; the blocker must never block these. */
    fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun label(packageName: String): String = runCatching {
        @Suppress("DEPRECATION")
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun icon(packageName: String): Drawable? =
        runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)
}
