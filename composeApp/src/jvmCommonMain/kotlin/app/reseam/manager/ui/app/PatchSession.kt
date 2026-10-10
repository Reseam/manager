package app.reseam.manager.ui.app

import app.reseam.manager.AppGraph
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.VersionOption
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.lookOf
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Starts from the version most patches support, never the installed copy: it may already be patched. */
class PatchSession(private val graph: AppGraph, val packageName: String, picked: AppSource?) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val appState = MutableStateFlow(AppLook(packageName, packageName))
    val app: StateFlow<AppLook> = appState.asStateFlow()

    val versions: StateFlow<List<VersionOption>> = graph.bundles.catalog.filterNotNull()
        .map { it.versions(packageName) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val sourceState = MutableStateFlow(picked)

    val source: StateFlow<AppSource?> = sourceState.asStateFlow()

    private val editorState = MutableStateFlow<PatchEditor?>(null)

    val editor: StateFlow<PatchEditor?> = editorState.asStateFlow()

    private val mountState = MutableStateFlow(false)
    val mount: StateFlow<Boolean> = mountState.asStateFlow()

    init {
        scope.launch { appState.value = graph.lookOf(packageName) }
        scope.launch {
            val mounted = graph.patchedApps.find(packageName).first()?.installMethod == InstallMethod.MOUNT
            mountState.value = mounted && graph.settings.settings.value.mountWithRoot
        }
        scope.launch {
            if (sourceState.value != null) return@launch
            val recommended = versions.first { it.isNotEmpty() }.first().versionName
            val saved = graph.savedApks.apks.first().firstOrNull { it.packageName == packageName && recommended != null && it.versionName == recommended }
            sourceState.value = saved?.let { AppSource.Saved(it.id, it.versionName) } ?: AppSource.Download(recommended)
        }
        scope.launch {
            combine(graph.bundles.patches.filterNotNull(), sourceState.filterNotNull().map { it.versionName }.distinctUntilChanged()) { patches, version -> patches to version }
                .collect { (patches, version) ->
                    editorState.value = PatchEditor.from(patches.values.flatten(), packageName, version, graph.settings.settings.value.allowIncompatiblePatches)
                }
        }
        scope.launch {
            graph.savedApks.apks.collect { apks ->
                val saved = sourceState.value as? AppSource.Saved ?: return@collect
                if (apks.none { it.id == saved.id }) sourceState.value = AppSource.Download(saved.versionName)
            }
        }
        scope.launch {
            graph.settings.settings.map { it.allowIncompatiblePatches }.distinctUntilChanged().collect { allowed -> edit { it.allow(allowed) } }
        }
    }

    fun choose(source: AppSource) {
        sourceState.value = source
    }

    fun setMount(mount: Boolean) {
        mountState.value = mount
    }

    fun toggle(reference: String, enabled: Boolean) = edit { it.toggle(reference, enabled) }

    fun setOption(reference: String, key: String, value: OptionValue?) = edit { it.setOption(reference, key, value) }

    fun apply(preset: PatchPreset) = edit { it.apply(preset) }

    private fun edit(transform: (PatchEditor) -> PatchEditor) = editorState.update { it?.let(transform) }

    override fun close() = scope.cancel()
}

class PatchSessions(private val graph: AppGraph) {
    private val open = mutableMapOf<String, PatchSession>()

    fun get(packageName: String, picked: AppSource? = null): PatchSession =
        open[packageName]?.also { session -> picked?.let(session::choose) } ?: PatchSession(graph, packageName, picked).also { open[packageName] = it }

    fun retain(packageNames: Set<String>) {
        open.keys.filterNot { it in packageNames }.forEach { open.remove(it)?.close() }
    }
}
