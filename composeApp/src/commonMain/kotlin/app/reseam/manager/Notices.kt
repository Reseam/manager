package app.reseam.manager

import app.reseam.manager.sdk.Problem
import app.reseam.manager.sdk.ReseamException

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Notice(val message: String, val id: Long)

/** Transient, app-wide messages for failures that happen outside a screen's own flow. */
class Notices {
    private val current = MutableStateFlow<Notice?>(null)
    private var counter = 0L

    val notice: StateFlow<Notice?> = current.asStateFlow()

    fun post(message: String) {
        current.value = Notice(message, ++counter)
    }

    fun dismiss(notice: Notice) {
        current.compareAndSet(notice, null)
    }
}

fun Throwable.userMessage(): String = when (this) {
    is ReseamException -> problem.userMessage() ?: message.orEmpty()
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
    Problem.Other -> null
}
