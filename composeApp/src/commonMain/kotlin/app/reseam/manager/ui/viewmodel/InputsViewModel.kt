package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.ReseamInput
import app.reseam.manager.patcher.ReseamManagerCore
import app.reseam.manager.ui.model.AppView
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.PatchEditorFactory
import app.reseam.manager.ui.model.PatchInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class InputsViewModel internal constructor(
    private val store: ManagerStateStore,
    private val core: ReseamManagerCore,
    private val patchStore: PatchStore,
    private val scope: CoroutineScope,
) {
    private var inspectJob: Job? = null

    fun startNewPatch() {
        inspectJob?.cancel()
        store.update { it.copy(backStack = listOf(AppView.Home, AppView.Flow.Choosing()), error = null) }
    }

    fun repatch(appId: String) {
        val app = store.state.home.patchedApps.firstOrNull { it.id == appId } ?: return
        startInspect(PatchInput.ApkFile(app.name, app.artifactPath))
    }

    fun setInputMode(mode: InputMode) = mutateChoosing { it.copy(mode = mode, selected = null) }

    fun setSearchQuery(query: String) = mutateChoosing { it.copy(query = query) }

    fun selectInstalledApp(appId: String) {
        val app = store.state.home.installedApps.firstOrNull { it.id == appId && it.hasCompatiblePatches }
        mutateChoosing { it.copy(selected = app?.let(PatchInput::InstalledApp)) }
    }

    fun selectApkFile(displayName: String, apkPath: String, splitPaths: List<String> = emptyList()) {
        mutateChoosing {
            it.copy(
                mode = InputMode.File,
                selected = PatchInput.ApkFile(displayName, apkPath, splitPaths),
            )
        }
    }

    fun continueToPatches() {
        val choosing = store.state.view as? AppView.Flow.Choosing ?: return
        if (choosing.inspecting) return
        val input = choosing.selected ?: return
        startInspect(input)
    }

    private fun startInspect(input: PatchInput) {
        inspectJob?.cancel()
        store.update {
            val choosing = (it.view as? AppView.Flow.Choosing)?.copy(selected = input, inspecting = true)
                ?: AppView.Flow.Choosing(
                    mode = if (input is PatchInput.InstalledApp) InputMode.Installed else InputMode.File,
                    selected = input,
                    inspecting = true,
                )
            val stack = if (it.view is AppView.Flow.Choosing) it.backStack.dropLast(1) else it.backStack
            it.copy(backStack = stack + choosing, error = null)
        }
        inspectJob = scope.launch {
            when (val result = core.inspect(buildInspectRequest(input))) {
                is ReseamCallResult.Success -> onInspectSuccess(input, result.value)
                is ReseamCallResult.Failure -> onInspectFailure(result.message)
            }
        }
    }

    private fun mutateChoosing(transform: (AppView.Flow.Choosing) -> AppView.Flow.Choosing) {
        store.update {
            val current = it.view as? AppView.Flow.Choosing ?: return@update it
            it.copy(backStack = it.backStack.dropLast(1) + transform(current), error = null)
        }
    }

    private suspend fun onInspectSuccess(input: PatchInput, response: InspectResponse) {
        val packageName = (input as? PatchInput.InstalledApp)?.app?.packageName ?: response.apk?.packageName
        val cached = if (packageName != null) patchStore.listByPackage(packageName) else emptyList()
        val editor = PatchEditorFactory.create(response, packageName, cached)
        store.update {
            val current = it.view as? AppView.Flow.Choosing ?: return@update it
            if (current.selected != input) return@update it
            it.copy(
                backStack = it.backStack.dropLast(1) +
                    current.copy(inspecting = false) +
                    AppView.Flow.Editing(input, editor),
            )
        }
    }

    private fun onInspectFailure(message: String) {
        store.update {
            val current = it.view as? AppView.Flow.Choosing ?: return@update it
            it.copy(
                backStack = it.backStack.dropLast(1) + current.copy(inspecting = false),
                error = message,
            )
        }
    }

    private fun buildInspectRequest(input: PatchInput) = ReseamInput(
        apkPath = input.apkPath,
        splitPaths = input.splitPaths,
        bundlePaths = store.state.bundles.installed.mapNotNull { it.path },
        trust = store.state.bundles.toTrustConfig(),
    )
}
