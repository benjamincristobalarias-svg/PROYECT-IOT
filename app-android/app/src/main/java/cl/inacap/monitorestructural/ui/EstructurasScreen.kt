// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.ui.theme.Amarillo
import cl.inacap.monitorestructural.ui.theme.Rojo
import cl.inacap.monitorestructural.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EstructurasScreen(state: EstructurasUiState) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Monitor estructural",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("Simulación local · Actualización cada 3 s")
                    Text(
                        "${state.estructuras.size} estructuras · " +
                                "${state.connectedCount} conectadas · " +
                                "${state.alertCount} con daño · " +
                                "${state.sismoCount} con sismo"
                    )
                }
            }
            when {
                state.isLoading -> item {
                    CircularProgressIndicator()
                }
                state.errorMessage != null -> item {
                    Text(
                        state.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.estructuras.isEmpty() -> item {
                    Text("Todavía no hay estructuras registradas.")
                }
                else -> items(
                    items = state.estructuras, key = { it.id }
                ) { estructura ->
                    EstructuraCard(estructura)
                }
            }
        }
    }
}

@Composable
private fun EstructuraCard(estructura: Estructura) {
    val connected =
        estructura.connection == ConnectionStatus.CONNECTED
    val lectura = estructura.lectura
    // Estado de la estructura: el texto y el color dicen lo mismo
    val estado = when {
        !connected -> "Sin evaluación actual"
        lectura == null -> "Sin datos"
        estructura.hasCurrentAlert -> "Daño: pérdida de rigidez"
        estructura.hasSismo -> "Vibración fuerte (sismo)"
        else -> "Normal"
    }
    val colorEstado = when {
        !connected || lectura == null -> MaterialTheme.colorScheme.onSurface
        estructura.hasCurrentAlert -> Rojo
        estructura.hasSismo -> Amarillo
        else -> Verde
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                estructura.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text("${estructura.id} · ${estructura.ubicacion}")
            Text(
                if (connected) "Conectado" else "Desconectado",
                color = if (connected)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            if (lectura == null) {
                Text("Sin lecturas disponibles")
            } else {
                if (!connected) {
                    Text("Última lectura guardada; no es actual")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frecuencia")
                        Text(
                            String.format(
                                Locale.getDefault(), "%.1f Hz",
                                lectura.frecuenciaHz
                            ),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Línea base")
                        Text(
                            String.format(
                                Locale.getDefault(), "%.1f Hz",
                                estructura.frecuenciaBaseHz
                            ),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Text(
                    String.format(
                        Locale.getDefault(), "Caída: %.1f %%",
                        estructura.caidaPorcentaje ?: 0.0
                    )
                )
                Text("Lectura: ${formatTime(lectura.measuredAtMillis)}")
            }
            Text(
                estado,
                color = colorEstado,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        .format(Date(epochMillis))
