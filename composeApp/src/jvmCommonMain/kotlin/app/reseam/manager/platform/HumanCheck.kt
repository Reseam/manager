package app.reseam.manager.platform

import androidx.compose.runtime.Composable

@Composable
expect fun HumanCheck(url: String, onVerified: () -> Unit)
