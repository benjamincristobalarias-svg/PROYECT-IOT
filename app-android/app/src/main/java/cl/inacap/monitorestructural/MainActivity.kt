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
import cl.inacap.monitorestructural.ui.EstructurasScreen
import cl.inacap.monitorestructural.ui.EstructurasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Punto de sustitución de la fuente de datos.
        // Hoy: el simulador. Después: Firebase o el sensor real.
        val repository: EstructurasRepository = FakeEstructurasRepository()

        val factory = viewModelFactory {
            initializer { EstructurasViewModel(repository) }
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
