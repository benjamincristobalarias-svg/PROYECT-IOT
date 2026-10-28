// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.monitorestructural.data.FirebaseEstructurasRepository
import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Todo lo que la pantalla de Monitoreo necesita mostrar
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
    firebase: FirebaseEstructurasRepository
) : ViewModel() {

    // LECTURA: la pantalla muestra lo que hay guardado en Firebase
    // (lo que envía el Activador desde este u otro celular)
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

    // CONEXIÓN: true mientras el celular está conectado a Firebase
    val conectado: StateFlow<Boolean> = firebase
        .observeConexion()
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
}
