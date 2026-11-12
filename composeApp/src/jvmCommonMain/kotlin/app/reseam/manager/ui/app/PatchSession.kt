package app.reseam.manager.ui.app

import app.reseam.manager.AppGraph
import app.reseam.manager.PatchEditor
import app.reseam.manager.data.AndroidFit
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.Fit
import app.reseam.manager.data.VersionOption
import app.reseam.manager.data.preferredFor
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.lookOf
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.PatchRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Starts from the version most patches support that runs on the device, never the installed copy: it may already be
 * patched. Patches start as the app's newest build has them, so patching again keeps the user's choice, as an update does.
 */
class PatchSession(private val graph: AppGraph, val packageName: String, picked: AppSource?) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val appState = MutableStateFlow(AppLook(packageName, packageName))
    val app: StateFlow<AppLook> = appState.asStateFlow()

    private val catalogVersions = graph.bundles.catalog.filterNotNull().map { it.versions(packageName) }

    val versions: StateFlow<List<VersionOption>> = combine(catalogVersions, graph.androidRequirements.known) { options, known ->
        graph.androidRequirements.fit(packageName, options, known)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val checked = MutableStateFlow(false)

    private val sourceState = MutableStateFlow(picked)

    val source: StateFlow<AppSource?> = sourceState.asStateFlow()

    private val editorState = MutableStateFlow<PatchEditor?>(null)

    val editor: StateFlow<PatchEditor?> = editorState.asStateFlow()

    private val editedState = MutableStateFlow(picked != null)

    /** The user picked the source or patches here, so a run follows this page instead of what an update would take. */
    val edited: StateFlow<Boolean> = editedState.asStateFlow()

    private val mountState = MutableStateFlow(false)
    val mount: StateFlow<Boolean> = mountState.asStateFlow()

    private val fitState = MutableStateFlow(Fit.Device)

    /** Who the build is for, as the app's last build was made unless the user picks another. */
    val fit: StateFlow<Fit> = fitState.asStateFlow()

    init {
        scope.launch { appState.value = graph.lookOf(packageName) }
        scope.launch {
            val mounted = graph.patchedApps.builds(packageName).first().firstOrNull()?.installMethod == InstallMethod.MOUNT
            mountState.value = mounted && graph.settings.settings.value.mountWithRoot
            edit { it.mount(mountState.value) }
        }
        scope.launch {
            catalogVersions.collectLatest { options ->
                graph.androidRequirements.check(packageName, options.mapNotNull { it.versionName })
                checked.value = true
            }
        }
        scope.launch {
            if (sourceState.value != null) return@launch
            val options = combine(versions, checked, ::Pair)
                .first { (options, checked) -> options.isNotEmpty() && (checked || options.first().android != AndroidFit.Unknown) }
                .first
            val recommended = options.first().versionName
            val saved = graph.savedApks.apks.first().preferredFor(packageName, recommended)
            sourceState.update { it ?: saved?.let { apk -> AppSource.Saved(apk.id, apk.versionName) } ?: AppSource.Download(recommended) }
        }
        scope.launch {
            val build = graph.patchedApps.builds(packageName).first().maxByOrNull { it.patchedAtEpochMs }
            if (!editedState.value) build?.let { fitState.value = it.fit }
            combine(graph.bundles.active.filterNotNull(), sourceState.filterNotNull().map { it.versionName }.distinctUntilChanged()) { patches, version -> patches to version }
                .collect { (patches, version) ->
                    val all = patches.values.flatten()
                    val allowed = graph.settings.settings.value.allowIncompatiblePatches
                    val editor = build?.let { PatchEditor.of(it, all, version, allowed) } ?: PatchEditor.from(all, packageName, version, allowed)
                    editorState.value = editor.mount(mountState.value)
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
        editedState.value = true
    }

    fun setFit(fit: Fit) {
        fitState.value = fit
        editedState.value = true
    }

    fun setMount(mount: Boolean) {
        mountState.value = mount
        edit { it.mount(mount) }
        editedState.value = true
    }

    fun toggle(reference: PatchRef, enabled: Boolean) = userEdit { it.toggle(reference, enabled) }

    fun setOption(reference: PatchRef, key: String, value: OptionValue?) = userEdit { it.setOption(reference, key, value) }

    fun apply(preset: PatchPreset) = userEdit { it.apply(preset) }

    private fun userEdit(transform: (PatchEditor) -> PatchEditor) {
        edit(transform)
        editedState.value = true
    }

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
