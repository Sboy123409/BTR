package com.example.btrapp.blocking.web

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.btrapp.blocking.appLockGraph

/** Brings the web filter back after a restart, for phones without Android's Always-on VPN set. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val canStart = WebFilterVpnService.consentIntent(context) == null
        if (canStart && context.appLockGraph.webFilterSettings.settle()) WebFilterVpnService.start(context)
    }
}
