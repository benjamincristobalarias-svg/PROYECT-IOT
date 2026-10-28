// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.monitorestructural.model.ESTRUCTURAS_INSTALADAS
import cl.inacap.monitorestructural.model.armarEstructura
import java.util.Locale
import kotlin.math.roundToInt

// Pantalla Activador: hace de sensor y envía lecturas a Firebase.
// El otro celular las recibe en la pantalla de Monitoreo.
@Composable
fun ActivadorScreen(
    state: ActivadorUiState,
    onEnviar: (id: String, frecuenciaHz: Double, amplitud: Double, conectado: Boolean) -> Unit,
    onAutomatico: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Lo que se elige en los controles (se mantiene al girar el celular)
    var seleccion by rememberSaveable { mutableStateOf(ESTRUCTURAS_INSTALADAS.first().id) }
    var frecuencia by rememberSaveable {
        mutableStateOf(ESTRUCTURAS_INSTALADAS.first().frecuenciaBaseHz.toFloat())
    }
    var amplitud by rememberSaveable { mutableStateOf(150_000f) }
    var conectado by rememberSaveable { mutableStateOf(true) }
    // Con el envío automático encendido, los controles manuales se bloquean
    val manual = !state.automatico

    // Frecuencia con 1 decimal y amplitud de 1.000 en 1.000
    val frecuenciaHz = (frecuencia * 10).roundToInt() / 10.0
    val amplitudRedondeada = (amplitud / 1000).roundToInt() * 1000.0
    // Lo que verá el Monitoreo con estos valores (mismas reglas del ESP32)
    val vistaPrevia = armarEstructura(seleccion, frecuenciaHz, amplitudRedondeada, conectado, 0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Activador", style = MaterialTheme.typography.headlineSmall)
        Text("Hace de sensor: elige los valores y envíalos. El otro celular los ve en Monitoreo.")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("1. Estructura", style = MaterialTheme.typography.titleMedium)
                ESTRUCTURAS_INSTALADAS.forEach { estructura ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = estructura.id == seleccion,
                            onClick = {
                                seleccion = estructura.id
                                frecuencia = estructura.frecuenciaBaseHz.toFloat()
                            },
                            enabled = manual
                        )
                        Text("${estructura.nombre} (${estructura.id})")
                    }
                }
                if (vistaPrevia != null) {
                    Text(
                        String.format(
                            Locale.getDefault(),
                            "Línea base: %.1f Hz (se registra en terreno, no se cambia desde la app)",
                            vistaPrevia.frecuenciaBaseHz
                        )
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("2. Lectura del sensor", style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.getDefault(), "Frecuencia: %.1f Hz", frecuenciaHz))
                Slider(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    valueRange = 10f..50f,
                    enabled = manual
                )
                Text(String.format(Locale.getDefault(), "Amplitud: %,.0f", amplitudRedondeada))
                Slider(
                    value = amplitud,
                    onValueChange = { amplitud = it },
                    valueRange = 0f..400_000f,
                    enabled = manual
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sensor conectado", modifier = Modifier.weight(1f))
                    Switch(
                        checked = conectado,
                        onCheckedChange = { conectado = it },
                        enabled = manual
                    )
                }
                if (vistaPrevia != null) {
                    Text(
                        "El Monitoreo mostrará: ${textoEstado(vistaPrevia)}",
                        color = colorEstado(vistaPrevia),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = { onEnviar(seleccion, frecuenciaHz, amplitudRedondeada, conectado) },
                    enabled = manual,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enviar a Firebase")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Envío automático", style = MaterialTheme.typography.titleMedium)
                    Text("El simulador envía las 3 estructuras cada 3 s")
                }
                Switch(checked = state.automatico, onCheckedChange = onAutomatico)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("3. Estado del envío", style = MaterialTheme.typography.titleMedium)
                Text(state.mensaje)
                Text("Enviados: ${state.enviados} · Confirmados: ${state.confirmados} · Errores: ${state.errores}")
                Text(
                    "Último: ${state.ultimoMs?.let { "$it ms" } ?: "—"} · " +
                            "Promedio: ${state.promedioMs?.let { "$it ms" } ?: "—"}"
                )
            }
        }
    }
}
