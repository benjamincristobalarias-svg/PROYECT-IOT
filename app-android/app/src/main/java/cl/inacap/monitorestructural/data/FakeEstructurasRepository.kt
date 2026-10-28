// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.data

import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.model.LecturaVibracion
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

// Simula tres sensores que envían una lectura cada 3 segundos
class FakeEstructurasRepository(
    private val intervalMillis: Long = 3_000L,
    private val nowMillis: () -> Long = {
        System.currentTimeMillis()
    }
) : EstructurasRepository {

    init {
        require(intervalMillis > 0)
    }

    override fun observeEstructuras(): Flow<List<Estructura>> = flow {
        var previous = emptyList<Estructura>()
        var phase = 0
        while (currentCoroutineContext().isActive) {
            previous = simulatedSnapshot(
                phase, previous, nowMillis()
            )
            emit(previous)
            delay(intervalMillis)
            phase = (phase + 1) % 6
        }
    }
}

// Función pura: arma la lista de estructuras de cada fase (0 a 5).
// Fase 2, 3 y 4: Edificio B pierde rigidez (daño).
// Fase 5: Edificio A recibe un sismo.
// Fase 0 y 4: Bodega Norte se desconecta y conserva su última lectura.
internal fun simulatedSnapshot(
    phase: Int,
    previous: List<Estructura>,
    nowMillis: Long
): List<Estructura> {
    require(phase in 0..5)
    val oldBodega = previous.find { it.id == "NODO-03" }
    val bodegaConnected = phase != 0 && phase != 4

    return listOf(
        Estructura(
            id = "NODO-01",
            nombre = "Edificio A",
            ubicacion = "Viga piso 2, eje C",
            frecuenciaBaseHz = 40.0,
            connection = ConnectionStatus.CONNECTED,
            lectura = LecturaVibracion(
                frecuenciaHz = 40.0 - phase * 0.2,
                amplitud = if (phase == 5) 260_000.0 else 150_000.0,
                measuredAtMillis = nowMillis
            )
        ),
        Estructura(
            id = "NODO-02",
            nombre = "Edificio B",
            ubicacion = "Columna planta baja, eje A",
            frecuenciaBaseHz = 35.0,
            connection = ConnectionStatus.CONNECTED,
            lectura = LecturaVibracion(
                frecuenciaHz = if (phase in 2..4) 30.0 else 34.5,
                amplitud = 140_000.0,
                measuredAtMillis = nowMillis
            )
        ),
        Estructura(
            id = "NODO-03",
            nombre = "Bodega Norte",
            ubicacion = "Muro norte, nivel 1",
            frecuenciaBaseHz = 28.0,
            connection = if (bodegaConnected)
                ConnectionStatus.CONNECTED
            else ConnectionStatus.DISCONNECTED,
            lectura = if (bodegaConnected)
                LecturaVibracion(
                    frecuenciaHz = 28.0 - phase * 0.1,
                    amplitud = 120_000.0,
                    measuredAtMillis = nowMillis
                )
            else oldBodega?.lectura
        )
    )
}
