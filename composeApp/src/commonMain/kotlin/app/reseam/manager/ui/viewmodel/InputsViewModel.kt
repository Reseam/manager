package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.ReseamInput
import app.reseam.manager.patcher.ReseamManagerCore
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.PatchEditorFactory
import app.reseam.manager.ui.model.PatchFlowState
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.model.navigation.ManagerRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@Stable
class InputsViewModel internal constructor(
    private val store: ManagerStateStore,
    private val core: ReseamManagerCore,
    private val patchStore: PatchStore,
    private val scope: CoroutineScope,
) {
    fun setInputMode(mode: InputMode) {
        updateFlowResettingInspect { it.copy(inputMode = mode, selectedInput = null) }
    }

    fun setSearchQuery(query: String) {
        store.update { it.copy(flow = it.flow.copy(searchQuery = query)) }
    }

    fun selectInstalledApp(appId: String) {
        val app = store.state.flow.installedApps.firstOrNull { it.id == appId && it.hasCompatiblePatches }
        updateFlowResettingInspect { it.copy(selectedInput = app?.let(PatchInput::InstalledApp)) }
    }

    private fun updateFlowResettingInspect(mutate: (PatchFlowState) -> PatchFlowState) {
        store.update {
            it.copy(flow = mutate(it.flow).copy(inspect = LoadState.Idle), error = null)
        }
    }

    fun selectApkFile(displayName: String, apkPath: String, splitPaths: List<String> = emptyList()) {
        updateFlowResettingInspect {
            it.copy(
                inputMode = InputMode.File,
                selectedInput = PatchInput.ApkFile(displayName, apkPath, splitPaths),
            )
        }
    }

    fun selectWebDownload(displayName: String, apkPath: String, sourceUrl: String, splitPaths: List<String> = emptyList()) {
        updateFlowResettingInspect {
            it.copy(
                inputMode = InputMode.Web,
                selectedInput = PatchInput.WebDownload(displayName, apkPath, splitPaths, sourceUrl),
            )
        }
    }

    fun continueToPatches() {
        val flow = store.state.flow
        if (flow.inspect is LoadState.Loading) return

        val input = flow.selectedInput ?: return

        scope.launch {
            store.update { it.copy(flow = it.flow.copy(inspect = LoadState.Loading), error = null) }
            yield()
            when (val result = inspect(input)) {
                is ReseamCallResult.Success -> {
                    val packageName = (input as? PatchInput.InstalledApp)?.app?.packageName
                        ?: result.value.apk?.packageName
                    val cached = if (packageName != null) patchStore.listByPackage(packageName) else emptyList()
                    showPatches(input, result.value, packageName, cached)
                }
                is ReseamCallResult.Failure -> showInspectError(result.message)
            }
        }
    }

    private suspend fun inspect(input: PatchInput): ReseamCallResult<InspectResponse> =
        core.inspect(
            ReseamInput(
                apkPath = input.apkPath,
                splitPaths = input.splitPaths,
                bundlePaths = store.state.bundles.installed.mapNotNull { it.path },
                trust = store.state.bundles.toTrustConfig(),
            ),
        )

    private fun showPatches(
        input: PatchInput,
        inspect: InspectResponse,
        packageName: String?,
        cachedPatches: List<PatchMetadata>,
    ) {
        store.update {
            it.copy(
                navigation = it.navigation.push(ManagerRoute.Patches),
                flow = it.flow.copy(
                    inspect = LoadState.Loaded(inspect),
                    editor = PatchEditorFactory.create(input.displayName, inspect, packageName, cachedPatches),
                ),
            )
        }
    }

    private fun showInspectError(message: String) {
        store.update {
            it.copy(
                flow = it.flow.copy(inspect = LoadState.Failed(message)),
                error = message,
            )
        }
    }
}
