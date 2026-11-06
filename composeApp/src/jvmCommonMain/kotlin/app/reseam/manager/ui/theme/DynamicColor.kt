package app.reseam.manager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

internal expect val dynamicColorAvailable: Boolean

@Composable
internal expect fun dynamicColorScheme(dark: Boolean): ColorScheme?
