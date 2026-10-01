package app.reseam.manager

import app.reseam.sdk.Problem
import app.reseam.sdk.SdkError

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NoticeKind { Info, Warning }

data class Notice(val message: String, val kind: NoticeKind, val id: Long)

/** Transient, app-wide messages about work that happens outside a screen's own flow. */
class Notices {
    private val current = MutableStateFlow<Notice?>(null)
    private var counter = 0L

    val notice: StateFlow<Notice?> = current.asStateFlow()

    fun info(message: String) = post(message, NoticeKind.Info)

    fun warn(message: String) = post(message, NoticeKind.Warning)

    private fun post(message: String, kind: NoticeKind) {
        current.value = Notice(message, kind, ++counter)
    }

    fun dismiss(notice: Notice) {
        current.compareAndSet(notice, null)
    }
}

fun Throwable.userMessage(): String = when (this) {
    is SdkError -> problem.userMessage() ?: message.orEmpty()
    else -> message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "Unknown error"
}

/** Plain words for what went wrong and what to do; null when the engine text is the best we have. */
fun Problem.userMessage(bundleName: String? = null): String? = when (this) {
    is Problem.BundleTooOld -> "${bundleName ?: bundle} was made for an older version of Reseam. Update it to keep using its patches."
    is Problem.EngineTooOld -> "${bundleName ?: bundle} needs a newer Reseam Manager. Update the app to use it."
    is Problem.UntrustedBundle -> "${bundleName ?: "This bundle"} is signed by a key you haven't approved yet. Review it under Bundles."
    is Problem.UnreadableBundle -> "${bundleName ?: "This file"} isn't a bundle Reseam can read. Remove it and add it again."
    is Problem.UnreadableApk -> "This file isn't an app Reseam can open. Pick an APK, APKM, or XAPK."
    is Problem.PatchesFailed -> if (patches.size == 1) "1 patch couldn't be applied. The log below says why." else "${patches.size} patches couldn't be applied. The log below says why."
    is Problem.OptionType -> "Option '$key' has the wrong type. Review the patch options."
    is Problem.OptionChoice -> "Option '$key' must be one of: ${allowed.joinToString()}."
    is Problem.SingleFileComponents -> "This app has $components components. Save it as a split app."
    Problem.MissingPackage -> "This app has no package name. Pick another APK, APKM, or XAPK."
    is Problem.IncompatiblePackage -> "These patches don't support this app. Pick a compatible app."
    is Problem.UnknownPreset -> "The patch preset '$value' is unavailable. Select the patches again."
    Problem.Other -> null
}
