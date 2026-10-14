package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.data.Acknowledgement
import app.reseam.manager.data.acknowledgements
import app.reseam.manager.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LicensesViewModel : ViewModel() {
    private val librariesState = MutableStateFlow<List<Acknowledgement>?>(null)

    val libraries: StateFlow<List<Acknowledgement>?> = librariesState.asStateFlow()

    init {
        viewModelScope.launch {
            librariesState.value = withContext(Dispatchers.Default) { acknowledgements(Res.readBytes("files/licenses.json").decodeToString()) }
        }
    }
}
