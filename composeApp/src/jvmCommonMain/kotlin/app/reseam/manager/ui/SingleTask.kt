package app.reseam.manager.ui

import app.reseam.manager.Notices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Runs one task at a time and posts its failure; [running] is the task in progress. */
class SingleTask<T : Any>(private val scope: CoroutineScope, private val notices: Notices) {
    private val runningState = MutableStateFlow<T?>(null)
    val running: StateFlow<T?> = runningState.asStateFlow()
    val busy: StateFlow<Boolean> = running.map { it != null }.stateIn(scope, SharingStarted.Eagerly, false)

    fun launch(task: T, block: suspend () -> Unit) {
        if (!runningState.compareAndSet(null, task)) return
        scope.launch {
            try {
                notices.attempt { block() }
            } finally {
                runningState.value = null
            }
        }
    }
}
