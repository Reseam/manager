package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import app.reseam.manager.AppGraph
import app.reseam.manager.data.SourceImport
import app.reseam.manager.data.SourceInput
import kotlinx.coroutines.flow.StateFlow

class AddSourceViewModel(graph: AppGraph) : ViewModel() {
    private val imports = graph.sourceImports

    val state: StateFlow<SourceImport?> = imports.current

    fun edit(link: String) = imports.edit(link)

    fun add(link: String) = imports.open(SourceInput.Link(link))

    fun pick() = imports.pick()

    fun dismiss(session: Long) = imports.dismiss(session)
}
