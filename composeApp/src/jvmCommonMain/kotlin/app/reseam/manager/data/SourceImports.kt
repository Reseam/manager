package app.reseam.manager.data

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SourceInput {
    data class Link(val url: String) : SourceInput

    data class File(val file: PlatformFile) : SourceInput
}

/** Every import has its own [session], so a sheet closing late can't end the one that replaced it. */
sealed interface SourceImport {
    val session: Long

    /** [checking] is the input being read; [failure] is why the last one couldn't be added. */
    data class Entering(override val session: Long, val link: String = "", val checking: SourceInput? = null, val failure: ImportFailure? = null) : SourceImport

    /** [replacing] is the same publisher's copy added from a file or link, which this one replaces. */
    data class Reviewing(override val session: Long, val input: SourceInput, val staged: StagedBundle, val replacing: Bundle?, val saving: Boolean = false) : SourceImport

    data class Present(override val session: Long, val bundle: Bundle) : SourceImport
}

data class ImportFailure(val input: SourceInput, val error: Exception)

/** Adds a patch source from a shared link or a picked or opened file, one at a time, whichever screen is showing. */
class SourceImports(private val scope: CoroutineScope, private val bundles: BundleRepository) {
    private val state = MutableStateFlow<SourceImport?>(null)
    val current: StateFlow<SourceImport?> = state.asStateFlow()

    private val addedEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val added: SharedFlow<String> = addedEvents.asSharedFlow()

    private var checking: Job? = null
    private var sessions = 0L

    fun begin() {
        if (state.value == null) state.value = SourceImport.Entering(++sessions)
    }

    fun edit(link: String) = state.update { current -> if (current is SourceImport.Entering && current.checking == null) current.copy(link = link, failure = null) else current }

    fun pick() {
        scope.launch { FileKit.openFilePicker(type = FileKitType.File(listOf("reseam")))?.let { open(SourceInput.File(it)) } }
    }

    fun open(input: SourceInput) {
        close()
        val session = ++sessions
        val link = (input as? SourceInput.Link)?.url.orEmpty()
        state.value = SourceImport.Entering(session, link, checking = input)
        checking = scope.launch {
            state.value = try {
                review(session, input)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                SourceImport.Entering(session, link, failure = ImportFailure(input, error))
            }
        }
    }

    /** A source that already updates on its own is shown as added, before its bundle is downloaded when its index names the key. */
    private suspend fun review(session: Long, input: SourceInput): SourceImport {
        fun following(key: String) = bundles.installed().firstOrNull { it.id == key && it.followsUpdates }
        val staged = when (input) {
            is SourceInput.File -> bundles.stageFile(input.file)
            is SourceInput.Link -> when (val target = bundles.resolveLink(input.url)) {
                is LinkTarget.Bundle -> target.staged
                is LinkTarget.Index -> {
                    following(target.link.index.bundle.publicKey)?.let { return SourceImport.Present(session, it) }
                    bundles.stageIndex(target.link)
                }
            }
        }
        following(staged.metadata.publicKey)?.let {
            bundles.discard(staged)
            return SourceImport.Present(session, it)
        }
        return SourceImport.Reviewing(session, input, staged, bundles.installed().firstOrNull { it.id == staged.metadata.publicKey })
    }

    /** The save runs to the end even when the sheet closes or another source opens, so the library never holds half of it. */
    fun confirm() {
        val review = state.value as? SourceImport.Reviewing ?: return
        if (review.saving) return
        val saving = review.copy(saving = true)
        state.value = saving
        scope.launch {
            try {
                bundles.install(review.staged)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                bundles.discard(review.staged)
                state.compareAndSet(saving, SourceImport.Entering(review.session, (review.input as? SourceInput.Link)?.url.orEmpty(), failure = ImportFailure(review.input, error)))
                return@launch
            }
            addedEvents.emit(review.staged.metadata.publicKey)
            state.compareAndSet(saving, null)
        }
    }

    fun dismiss(session: Long) {
        if (state.value?.session == session) close()
    }

    private fun close() {
        checking?.cancel()
        val previous = state.getAndUpdate { null } as? SourceImport.Reviewing ?: return
        if (!previous.saving) scope.launch { bundles.discard(previous.staged) }
    }
}
