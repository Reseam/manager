package app.reseam.manager

import app.reseam.manager.data.SyncFailure
import app.reseam.sdk.Problem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface Notice {
    data object InstallCancelled : Notice
    data object InstallUnconfirmed : Notice
    data object InstallTimedOut : Notice
    data object AppMounted : Notice
    data object AppUnmounted : Notice
    data object RootRefused : Notice
    data class AppSaved(val fileName: String) : Notice
    data class BundleRemoved(val name: String, val problem: Problem) : Notice
    data class SignerToConfirm(val name: String) : Notice
    data class SyncFailed(val failures: List<SyncFailure>) : Notice
    data object KeyImported : Notice
    data class KeyExported(val fileName: String) : Notice
    data object KeyReset : Notice
    data class Error(val error: Throwable) : Notice
}

data class PostedNotice(val notice: Notice, val id: Long)

class Notices {
    private val current = MutableStateFlow<PostedNotice?>(null)
    private var counter = 0L

    val posted: StateFlow<PostedNotice?> = current.asStateFlow()

    fun post(notice: Notice) {
        current.value = PostedNotice(notice, ++counter)
    }

    fun error(error: Throwable) = post(Notice.Error(error))

    fun dismiss(posted: PostedNotice) {
        current.compareAndSet(posted, null)
    }

    /** Runs [block], posting any failure; null means it failed. */
    suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        error(error)
        null
    }
}
