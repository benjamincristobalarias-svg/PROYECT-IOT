// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.lifecycle.ViewModel
import cl.inacap.monitorestructural.data.AuthRepository
import cl.inacap.monitorestructural.model.validarCredenciales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Estado de la pantalla de Login
data class LoginUiState(
    val usuario: String? = null,   // correo del usuario con sesión; null = sin sesión
    val cargando: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    // Firebase recuerda la sesión: si ya había entrado antes, parte con su correo
    private val _uiState = MutableStateFlow(LoginUiState(usuario = auth.usuarioActual()))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun ingresar(correo: String, clave: String) = enviar(correo, clave, cuentaNueva = false)

    fun crearCuenta(correo: String, clave: String) = enviar(correo, clave, cuentaNueva = true)

    fun salir() {
        auth.salir()
        _uiState.value = LoginUiState()
    }

    private fun enviar(correo: String, clave: String, cuentaNueva: Boolean) {
        // Primero se revisan los datos en el celular (no se envía nada mal escrito)
        val error = validarCredenciales(correo, clave, cuentaNueva)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(cargando = true, error = null) }
        val alTerminar = { usuario: String?, mensaje: String? ->
            _uiState.value = LoginUiState(usuario = usuario, error = mensaje)
        }
        if (cuentaNueva) {
            auth.crearCuenta(correo.trim(), clave, alTerminar)
        } else {
            auth.ingresar(correo.trim(), clave, alTerminar)
        }
    }
}
