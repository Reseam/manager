package app.reseam.manager.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsState(
    val checkUpdatesDaily: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val theme: ThemeMode = ThemeMode.System,
    val onboardingCompleted: Boolean = false,
    val apiBaseUrl: String = DefaultApiBaseUrl,
)

const val DefaultApiBaseUrl: String = "https://api.reseam.app/v1"

@Serializable
enum class ThemeMode {
    System,
    Light,
    Dark,
}
