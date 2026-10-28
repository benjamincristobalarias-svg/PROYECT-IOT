// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.monitorestructural.data.EstructurasRepository
import cl.inacap.monitorestructural.data.FirebaseEstructurasRepository
import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Todo lo que la pantalla necesita mostrar
data class EstructurasUiState(
    val isLoading: Boolean = true,
    val estructuras: List<Estructura> = emptyList(),
    val errorMessage: String? = null
) {
    val connectedCount: Int
        get() = estructuras.count {
            it.connection == ConnectionStatus.CONNECTED
        }
    val alertCount: Int
        get() = estructuras.count { it.hasCurrentAlert }
    val sismoCount: Int
        get() = estructuras.count { it.hasSismo }
}

class EstructurasViewModel(
    firebase: FirebaseEstructurasRepository,
    simulador: EstructurasRepository
) : ViewModel() {

    init {
        // ENVÍO: cada lista nueva del simulador (cada 3 s) se guarda en Firebase
        viewModelScope.launch {
            simulador.observeEstructuras().collect { lista ->
                firebase.guardar(lista)
            }
        }
    }

    // LECTURA: la pantalla muestra lo que hay guardado en Firebase
    val uiState: StateFlow<EstructurasUiState> = firebase
        .observeEstructuras()
        .map { estructuras ->
            EstructurasUiState(
                isLoading = false, estructuras = estructuras
            )
        }
        .catch {
            emit(
                EstructurasUiState(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar los datos de Firebase."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstructurasUiState()
        )
}
