package app.reseam.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.reseam.manager.ui.ReseamManagerApp
import app.reseam.manager.ui.platform.rememberAndroidManagerViewModel
import app.reseam.manager.ui.platform.rememberAndroidPermissionHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val vm = rememberAndroidManagerViewModel()
            val permissionHandler = rememberAndroidPermissionHandler()
            ReseamManagerApp(vm, permissionHandler)
        }
    }
}
