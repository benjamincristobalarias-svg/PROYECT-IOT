// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// Pantalla después del Login: barra superior (usuario, conexión y Salir)
// y dos pestañas, Monitoreo y Activador
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalScreen(
    usuario: String,
    conectado: Boolean,
    onSalir: () -> Unit,
    monitoreo: @Composable (Modifier) -> Unit,
    activador: @Composable (Modifier) -> Unit
) {
    var pestana by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monitor Estructural")
                        Text(
                            "$usuario · " + (if (conectado) "Conectado a Firebase" else "Sin conexión"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conectado) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSalir) { Text("Salir") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Monitoreo") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Activador") })
            }
            if (pestana == 0) {
                monitoreo(Modifier.weight(1f))
            } else {
                activador(Modifier.weight(1f))
            }
        }
    }
}
