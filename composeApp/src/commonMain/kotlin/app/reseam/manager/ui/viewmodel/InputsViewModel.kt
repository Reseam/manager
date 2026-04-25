package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.ReseamInput
import app.reseam.manager.patcher.ReseamManagerCore
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.PatchEditorFactory
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.model.navigation.ManagerRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class InputsViewModel internal constructor(
    private val store: ManagerStateStore,
    private val core: ReseamManagerCore,
    private val scope: CoroutineScope,
) {
    fun setInputMode(mode: InputMode) {
        store.update { it.copy(flow = it.flow.copy(inputMode = mode, selectedInput = null)) }
    }

    fun setSearchQuery(query: String) {
        store.update { it.copy(flow = it.flow.copy(searchQuery = query)) }
    }

    fun selectInstalledApp(appId: String) {
        val app = store.state.flow.installedApps.firstOrNull { it.id == appId && it.hasCompatiblePatches }
        store.update { it.copy(flow = it.flow.copy(selectedInput = app?.let(PatchInput::InstalledApp))) }
    }

    fun selectApkFile(displayName: String, apkPath: String, splitPaths: List<String> = emptyList()) {
        store.update {
            it.copy(
                flow = it.flow.copy(
                    inputMode = InputMode.File,
                    selectedInput = PatchInput.ApkFile(displayName, apkPath, splitPaths),
                ),
            )
        }
    }

    fun selectWebDownload(displayName: String, apkPath: String, sourceUrl: String, splitPaths: List<String> = emptyList()) {
        store.update {
            it.copy(
                flow = it.flow.copy(
                    inputMode = InputMode.Web,
                    selectedInput = PatchInput.WebDownload(displayName, apkPath, splitPaths, sourceUrl),
                ),
            )
        }
    }

    fun continueToPatches() {
        val input = store.state.flow.selectedInput ?: return

        scope.launch {
            store.update { it.copy(flow = it.flow.copy(inspect = LoadState.Loading), error = null) }
            when (val result = inspect(input)) {
                is ReseamCallResult.Success -> showPatches(input, result.value)
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

    private fun showPatches(input: PatchInput, inspect: InspectResponse) {
        store.update {
            it.copy(
                navigation = it.navigation.push(ManagerRoute.Patches),
                flow = it.flow.copy(
                    inspect = LoadState.Loaded(inspect),
                    editor = PatchEditorFactory.create(input.displayName, inspect),
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
