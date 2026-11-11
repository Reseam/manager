package app.reseam.manager.platform

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/** Makes [language] the process's locale, or the device's from [configuration] when null. */
fun applyLanguage(language: String?, configuration: Configuration): Locale =
    (language?.let(Locale::forLanguageTag) ?: configuration.locales[0]).also(Locale::setDefault)

@Composable
actual fun rememberAppLocale(language: String?): Locale {
    // Android puts the device's locale back as the default on a configuration change, so each one applies it again.
    val configuration = LocalConfiguration.current
    return remember(language, configuration) { applyLanguage(language, configuration) }
}
