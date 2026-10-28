// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.monitorestructural.data.EstructurasRepository
import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    repository: EstructurasRepository
) : ViewModel() {
    val uiState: StateFlow<EstructurasUiState> = repository
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
                    errorMessage = "No se pudieron cargar los datos."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstructurasUiState()
        )
}
