package app.reseam.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.reseam.manager.platform.sourceInput
import app.reseam.manager.ui.ReseamApp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    private val graph get() = (application as ReseamApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        if (savedInstanceState == null) openSource(intent)
        setContent { ReseamApp(graph) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openSource(intent)
    }

    private fun openSource(intent: Intent) {
        intent.sourceInput()?.let(graph.sourceImports::open)
    }
}
