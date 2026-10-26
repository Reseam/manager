package app.reseam.manager.data

import app.reseam.manager.Notice
import app.reseam.manager.Notices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BundleSyncer(
    private val scope: CoroutineScope,
    private val bundles: BundleRepository,
    private val settings: SettingsRepository,
    private val notices: Notices,
) {
    private val failureState = MutableStateFlow<Throwable?>(null)
    val failure: StateFlow<Throwable?> = failureState.asStateFlow()

    fun sync(force: Boolean = false): Job = scope.launch {
        bundles.patches.filterNotNull().first()
        val settings = settings.settings.value
        val (prompt, failures) = try {
            bundles.sync(settings.serverUrl, install = force || settings.autoUpdateBundles)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failureState.value = error
            notices.error(error)
            return@launch
        }
        failureState.value = failures.firstOrNull()?.error
        if (failures.isNotEmpty()) notices.post(Notice.SyncFailed(failures))
        prompt?.let { notices.post(Notice.SignerToConfirm(it.metadata.name)) }
    }

    fun check(bundle: Bundle): Job = scope.launch {
        val sync = notices.attempt { bundles.sync(settings.settings.value.serverUrl, install = true, only = bundle.id) } ?: return@launch
        when {
            sync.failures.isNotEmpty() -> notices.post(Notice.SyncFailed(sync.failures))
            sync.prompt != null -> notices.post(Notice.SignerToConfirm(sync.prompt.metadata.name))
            sync.updated == 0 -> notices.post(Notice.UpToDate(bundle.name))
        }
    }

    fun update(offer: BundleOffer): Job = scope.launch {
        notices.attempt { bundles.update(offer) }?.let { notices.post(Notice.SignerToConfirm(it.metadata.name)) }
    }
}
