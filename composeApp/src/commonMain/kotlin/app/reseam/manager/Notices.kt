package app.reseam.manager

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

fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "Unknown error"
