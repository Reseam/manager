package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import app.reseam.manager.platform.Transfer
import app.reseam.manager.resources.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

@Composable
fun byteSize(bytes: Long): String {
    val units = listOf(Res.string.size_kb, Res.string.size_mb, Res.string.size_gb)
    if (bytes < 1024) return stringResource(Res.string.size_b, bytes.toString())
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = if (value < 10) 1 else 0 }.format(value)
    return stringResource(units[unit], number)
}

@Composable
fun versionLabel(versionName: String?): String =
    versionName?.let { stringResource(Res.string.version, it) } ?: stringResource(Res.string.version_unknown)

fun localDate(isoInstant: String): String =
    runCatching { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(Instant.parse(isoInstant)) }.getOrDefault(isoInstant)

fun String.fingerprint(length: Int = 16): String = take(length).chunked(4).joinToString(" ")

@Composable
fun downloadProgress(transfer: Transfer): String =
    transfer.total?.let { stringResource(Res.string.downloading_of, byteSize(transfer.written), byteSize(it)) } ?: stringResource(Res.string.downloading, byteSize(transfer.written))

@Composable
fun duration(duration: Duration): String {
    val seconds = duration.inWholeSeconds.coerceAtLeast(1)
    return if (seconds < 60) stringResource(Res.string.duration_seconds, seconds) else stringResource(Res.string.duration_minutes, seconds / 60, seconds % 60)
}
