package com.example.btrapp.blocking.ui.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.btrapp.appContainer
import com.example.btrapp.ui.theme.MyApplicationTheme

/** Full-screen cover shown over a locked app. Lives in its own task so "home" fully leaves it. */
class BlockActivity : ComponentActivity() {

    private var blockedPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        if (blockedPackage.isEmpty()) return finish()

        setContent {
            MyApplicationTheme {
                BlockScreen(
                    packageName = blockedPackage,
                    onGoHome = ::goHome,
                    onUnlocked = ::openBlockedApp,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_PACKAGE)?.let { blockedPackage = it }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun openBlockedApp() {
        appContainer.installedApps.launchIntent(blockedPackage)?.let(::startActivity)
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, packageName: String): Intent =
            Intent(context, BlockActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
    }
}
