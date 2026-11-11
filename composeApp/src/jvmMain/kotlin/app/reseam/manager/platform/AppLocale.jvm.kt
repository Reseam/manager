package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

private val deviceLocale: Locale = Locale.getDefault()

@Composable
actual fun rememberAppLocale(language: String?): Locale =
    remember(language) { (language?.let(Locale::forLanguageTag) ?: deviceLocale).also(Locale::setDefault) }
