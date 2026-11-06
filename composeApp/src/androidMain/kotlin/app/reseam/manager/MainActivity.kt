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
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.UpdateAppsAction
import app.reseam.manager.platform.sourceInput
import app.reseam.manager.ui.ReseamApp
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.reseamColors
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

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
            val colors = reseamColors(dark, settings.dynamicColor)
            val navigationBar = colors.scheme.surface.copy(alpha = Opacity.navigationBar).toArgb()
            // enableEdgeToEdge reads the system's dark mode, which the app's theme can override.
            LaunchedEffect(dark, navigationBar) {
                enableEdgeToEdge(
                    SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    SystemBarStyle.auto(navigationBar, navigationBar) { dark },
                )
            }
            ReseamApp(graph, colors)
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
