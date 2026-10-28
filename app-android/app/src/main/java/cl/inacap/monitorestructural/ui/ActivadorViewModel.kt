// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.monitorestructural.data.EstructurasRepository
import cl.inacap.monitorestructural.data.FirebaseEstructurasRepository
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.model.armarEstructura
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla Activador: cómo van los envíos a Firebase
data class ActivadorUiState(
    val automatico: Boolean = false,   // envío automático del simulador encendido
    val enviados: Int = 0,
    val confirmados: Int = 0,          // envíos que Firebase confirmó
    val errores: Int = 0,
    val ultimoMs: Long? = null,        // cuánto tardó Firebase en confirmar el último envío
    val totalMs: Long = 0,
    val mensaje: String = "Todavía no se envía nada."
) {
    val promedioMs: Long?
        get() = if (confirmados == 0) null else totalMs / confirmados
}

class ActivadorViewModel(
    private val firebase: FirebaseEstructurasRepository,
    private val simulador: EstructurasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivadorUiState())
    val uiState: StateFlow<ActivadorUiState> = _uiState.asStateFlow()

    private var simulacion: Job? = null

    // Envío manual: los valores que se eligieron con los controles de la pantalla
    fun enviarManual(id: String, frecuenciaHz: Double, amplitud: Double, conectado: Boolean) {
        val estructura = armarEstructura(
            id, frecuenciaHz, amplitud, conectado, System.currentTimeMillis()
        ) ?: return
        enviar(estructura)
    }

    // Envío automático: el simulador entrega las 3 estructuras cada 3 s
    fun cambiarAutomatico(encendido: Boolean) {
        simulacion?.cancel()
        simulacion = null
        if (encendido) {
            simulacion = viewModelScope.launch {
                simulador.observeEstructuras().collect { lista ->
                    lista.forEach { enviar(it) }
                }
            }
        }
        _uiState.update { it.copy(automatico = encendido) }
    }

    // Envía a Firebase y mide cuánto tarda en llegar la confirmación del servidor.
    // Sin internet, Firebase guarda el envío y lo manda solo cuando vuelve la conexión.
    private fun enviar(estructura: Estructura) {
        val inicio = System.currentTimeMillis()
        _uiState.update {
            it.copy(enviados = it.enviados + 1, mensaje = "Enviando ${estructura.nombre}...")
        }
        firebase.enviar(estructura)
            .addOnSuccessListener {
                val ms = System.currentTimeMillis() - inicio
                _uiState.update {
                    it.copy(
                        confirmados = it.confirmados + 1,
                        ultimoMs = ms,
                        totalMs = it.totalMs + ms,
                        mensaje = "${estructura.nombre}: Firebase confirmó en $ms ms"
                    )
                }
            }
            .addOnFailureListener {
                _uiState.update {
                    it.copy(errores = it.errores + 1, mensaje = "No se pudo enviar (¿sesión o permisos?)")
                }
            }
    }
}
