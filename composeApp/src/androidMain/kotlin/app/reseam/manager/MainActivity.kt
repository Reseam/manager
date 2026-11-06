package app.reseam.manager

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.UpdateAppsAction
import app.reseam.manager.platform.sourceInput
import app.reseam.manager.ui.ReseamApp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

// enableEdgeToEdge's own navigation bar scrims, for Android versions that can't darken the bar's buttons.
private val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {
    private val graph get() = (application as ReseamApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        if (savedInstanceState == null) handle(intent)
        findViewById<ViewGroup>(android.R.id.content).isFocusableInTouchMode = true
        setContent {
            val settings by graph.settings.settings.collectAsStateWithLifecycle()
            val dark = settings.theme.dark(isSystemInDarkTheme())
            // enableEdgeToEdge reads the system's dark mode, which the app's theme can override.
            LaunchedEffect(dark) {
                enableEdgeToEdge(SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }, SystemBarStyle.auto(LightScrim, DarkScrim) { dark })
            }
            ReseamApp(graph, dark, settings.dynamicColor)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        if (intent.action == UpdateAppsAction) graph.updateAllApps()
        intent.sourceInput()?.let(graph.sourceImports::open)
    }
}
