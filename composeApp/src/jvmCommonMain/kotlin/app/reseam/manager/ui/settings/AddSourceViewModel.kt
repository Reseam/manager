package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.SourceImport
import app.reseam.manager.data.SourceInput
import app.reseam.manager.sdk.declared
import app.reseam.manager.ui.lookOf
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AddSourceViewModel(private val graph: AppGraph) : ViewModel() {
    private val imports = graph.sourceImports

    val state: StateFlow<SourceImport?> = imports.current
    val added: SharedFlow<String> = imports.added

    /** Names of the apps under review this device knows, from installed, patched or saved copies; bundles only carry package names. */
    val apps: StateFlow<List<String>?> = imports.current
        .map { (it as? SourceImport.Reviewing)?.staged?.patches }
        .distinctUntilChanged()
        .map { patches ->
            patches?.filterNot { it.spec.hidden }?.flatMap { it.declared }?.map { it.`package` }?.distinct()
                ?.map { graph.lookOf(it) }?.filter { it.name != it.packageName }?.map { it.name }?.sortedBy { it.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun edit(link: String) = imports.edit(link)

    fun add(link: String) = imports.open(SourceInput.Link(link))

    fun pick() = imports.pick()

    fun confirm() = imports.confirm()

    fun dismiss(session: Long) = imports.dismiss(session)
}
