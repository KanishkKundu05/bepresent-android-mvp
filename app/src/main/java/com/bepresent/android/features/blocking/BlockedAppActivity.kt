package com.bepresent.android.features.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.features.intentions.IntentionManager
import com.bepresent.android.features.sessions.SessionManager
import com.bepresent.android.ui.theme.BePresentTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppActivity : ComponentActivity() {

    @Inject lateinit var intentionManager: IntentionManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var intentionDao: AppIntentionDao
    @Inject lateinit var sessionDao: PresentSessionDao
    @Inject lateinit var preferencesManager: PreferencesManager

    private var blockedPackage: String? by mutableStateOf(null)
    private var shieldType: String by mutableStateOf(SHIELD_INTENTION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RuntimeLog.i(TAG, "onCreate: intent=$intent")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                RuntimeLog.d(TAG, "Back pressed -> navigateHome()")
                navigateHome()
            }
        })

        if (!applyIntent(intent)) {
            RuntimeLog.e(TAG, "Missing blocked package extra; finishing")
            finish()
            return
        }

        setContent {
            BePresentTheme {
                val currentPackage = blockedPackage
                if (currentPackage != null) {
                    ShieldScreen(
                        blockedPackage = currentPackage,
                        shieldType = shieldType,
                        intentionManager = intentionManager,
                        sessionManager = sessionManager,
                        intentionDao = intentionDao,
                        sessionDao = sessionDao,
                        preferencesManager = preferencesManager,
                        onNavigateHome = { navigateHome() },
                        onFinish = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        RuntimeLog.i(TAG, "onNewIntent: intent=$intent")
        if (!applyIntent(intent)) {
            RuntimeLog.w(TAG, "onNewIntent ignored: missing extras")
        }
    }

    override fun onResume() {
        super.onResume()
        lastResumeAtMs = System.currentTimeMillis()
        RuntimeLog.i(
            TAG,
            "onResume: blockedPackage=$blockedPackage shieldType=$shieldType"
        )
    }

    override fun onDestroy() {
        RuntimeLog.i(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun applyIntent(sourceIntent: Intent?): Boolean {
        val pkg = sourceIntent?.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: return false
        val type = sourceIntent.getStringExtra(EXTRA_SHIELD_TYPE) ?: SHIELD_INTENTION
        blockedPackage = pkg
        shieldType = type
        RuntimeLog.i(TAG, "applyIntent: blockedPackage=$pkg shieldType=$type")
        return true
    }

    private fun navigateHome() {
        RuntimeLog.i(TAG, "navigateHome")
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val TAG = "BP_BlockAct"
        @Volatile
        var lastResumeAtMs: Long = 0L
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_SHIELD_TYPE = "shield_type"
        const val SHIELD_SESSION = "session"
        const val SHIELD_INTENTION = "intention"
        const val SHIELD_GOAL_REACHED = "goalReached"
        const val SHIELD_SCHEDULE = "schedule"
    }
}
