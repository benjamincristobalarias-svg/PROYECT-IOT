// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.inacap.monitorestructural.data.EstructurasRepository
import cl.inacap.monitorestructural.data.FakeEstructurasRepository
import cl.inacap.monitorestructural.data.FirebaseEstructurasRepository
import cl.inacap.monitorestructural.ui.EstructurasScreen
import cl.inacap.monitorestructural.ui.EstructurasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fuentes de datos:
        // - el simulador genera las lecturas cada 3 s (después las enviará el sensor real)
        // - Firebase guarda esas lecturas y la pantalla muestra lo que hay en Firebase
        val simulador: EstructurasRepository = FakeEstructurasRepository()
        val firebase = FirebaseEstructurasRepository()

        val factory = viewModelFactory {
            initializer { EstructurasViewModel(firebase, simulador) }
        }
        val viewModel = ViewModelProvider(this, factory)
            .get(EstructurasViewModel::class.java)

        setContent {
            MaterialTheme {
                val state by viewModel.uiState
                    .collectAsStateWithLifecycle()
                EstructurasScreen(state)
            }
        }
    }
}
