// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.inacap.monitorestructural.data.AuthRepository
import cl.inacap.monitorestructural.data.EstructurasRepository
import cl.inacap.monitorestructural.data.FakeEstructurasRepository
import cl.inacap.monitorestructural.data.FirebaseEstructurasRepository
import cl.inacap.monitorestructural.ui.ActivadorScreen
import cl.inacap.monitorestructural.ui.ActivadorViewModel
import cl.inacap.monitorestructural.ui.EstructurasViewModel
import cl.inacap.monitorestructural.ui.LoginScreen
import cl.inacap.monitorestructural.ui.LoginViewModel
import cl.inacap.monitorestructural.ui.MonitoreoScreen
import cl.inacap.monitorestructural.ui.PrincipalScreen
import cl.inacap.monitorestructural.ui.theme.MonitorEstructuralTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fuentes de datos:
        // - AuthRepository: inicio de sesión (Firebase Authentication)
        // - FirebaseEstructurasRepository: base de datos (Realtime Database)
        // - FakeEstructurasRepository: simulador para el envío automático
        val auth = AuthRepository()
        val firebase = FirebaseEstructurasRepository()
        val simulador: EstructurasRepository = FakeEstructurasRepository()

        val factory = viewModelFactory {
            initializer { LoginViewModel(auth) }
            initializer { EstructurasViewModel(firebase) }
            initializer { ActivadorViewModel(firebase, simulador) }
        }
        val provider = ViewModelProvider(this, factory)
        val loginViewModel = provider.get(LoginViewModel::class.java)
        val monitoreoViewModel = provider.get(EstructurasViewModel::class.java)
        val activadorViewModel = provider.get(ActivadorViewModel::class.java)

        setContent {
            MonitorEstructuralTheme {
                val sesion by loginViewModel.uiState.collectAsStateWithLifecycle()
                val usuario = sesion.usuario
                if (usuario == null) {
                    // Sin sesión solo se ve el Login
                    LoginScreen(
                        state = sesion,
                        onIngresar = loginViewModel::ingresar,
                        onCrearCuenta = loginViewModel::crearCuenta
                    )
                } else {
                    val conectado by monitoreoViewModel.conectado.collectAsStateWithLifecycle()
                    PrincipalScreen(
                        usuario = usuario,
                        conectado = conectado,
                        onSalir = {
                            activadorViewModel.cambiarAutomatico(false)
                            loginViewModel.salir()
                        },
                        monitoreo = { modifier ->
                            val state by monitoreoViewModel.uiState.collectAsStateWithLifecycle()
                            MonitoreoScreen(state, modifier)
                        },
                        activador = { modifier ->
                            val state by activadorViewModel.uiState.collectAsStateWithLifecycle()
                            ActivadorScreen(
                                state = state,
                                onEnviar = activadorViewModel::enviarManual,
                                onAutomatico = activadorViewModel::cambiarAutomatico,
                                modifier = modifier
                            )
                        }
                    )
                }
            }
        }
    }
}
