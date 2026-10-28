// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural

import cl.inacap.monitorestructural.data.simulatedSnapshot
import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.model.LecturaVibracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas locales del modelo y del simulador (se ejecutan en el PC, sin celular)
class SimulacionTest {

    private fun buscar(lista: List<Estructura>, id: String) = lista.first { it.id == id }

    @Test
    fun alInicio_bodegaNoTieneLectura() {
        val lista = simulatedSnapshot(0, emptyList(), 1_000L)
        val bodega = buscar(lista, "NODO-03")
        assertEquals(ConnectionStatus.DISCONNECTED, bodega.connection)
        assertNull(bodega.lectura)
    }

    @Test
    fun fase2_edificioBTieneDanio() {
        val lista = simulatedSnapshot(2, emptyList(), 1_000L)
        assertTrue(buscar(lista, "NODO-02").hasCurrentAlert)
        assertFalse(buscar(lista, "NODO-01").hasCurrentAlert)
    }

    @Test
    fun fase5_edificioATieneSismoPeroNoDanio() {
        val lista = simulatedSnapshot(5, emptyList(), 1_000L)
        val edificioA = buscar(lista, "NODO-01")
        assertTrue(edificioA.hasSismo)
        assertFalse(edificioA.hasCurrentAlert)
    }

    @Test
    fun alDesconectarse_bodegaConservaSuUltimaLectura() {
        val fase3 = simulatedSnapshot(3, emptyList(), 9_000L)
        val fase4 = simulatedSnapshot(4, fase3, 12_000L)
        val bodega = buscar(fase4, "NODO-03")
        assertEquals(ConnectionStatus.DISCONNECTED, bodega.connection)
        assertEquals(9_000L, bodega.lectura?.measuredAtMillis)
        assertFalse(bodega.hasCurrentAlert)   // una lectura antigua no cuenta como alerta
    }

    @Test
    fun caidaJustoDe5PorCiento_noEsDanio() {
        val estructura = Estructura(
            id = "X", nombre = "Prueba", ubicacion = "-", frecuenciaBaseHz = 40.0,
            connection = ConnectionStatus.CONNECTED,
            lectura = LecturaVibracion(frecuenciaHz = 38.0, amplitud = 100_000.0, measuredAtMillis = 0L)
        )
        assertEquals(5.0, estructura.caidaPorcentaje ?: 0.0, 0.001)
        assertFalse(estructura.hasCurrentAlert)
    }
}
