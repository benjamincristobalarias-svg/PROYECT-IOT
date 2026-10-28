// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.model

// Umbrales del proyecto (los mismos del ESP32 de la Unidad 1)
const val UMBRAL_CAIDA_PORCENTAJE = 5.0   // si la frecuencia baja más de 5 % -> daño
const val AMPLITUD_SISMO = 230_000.0      // vibración muy fuerte -> sismo

enum class ConnectionStatus {
    CONNECTED, DISCONNECTED
}

// Una medición del sensor: frecuencia natural, amplitud de la vibración y hora
data class LecturaVibracion(
    val frecuenciaHz: Double,
    val amplitud: Double,
    val measuredAtMillis: Long
)

data class Estructura(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val frecuenciaBaseHz: Double,   // frecuencia de la estructura sana (línea base)
    val connection: ConnectionStatus,
    val lectura: LecturaVibracion? = null
) {
    // Cuánto bajó la frecuencia respecto de la línea base, en %. null = sin lectura
    val caidaPorcentaje: Double?
        get() = if (lectura == null) null
        else (frecuenciaBaseHz - lectura.frecuenciaHz) / frecuenciaBaseHz * 100

    // Daño: está conectada y la frecuencia bajó más que el umbral
    val hasCurrentAlert: Boolean
        get() {
            val caida = caidaPorcentaje
            return connection == ConnectionStatus.CONNECTED &&
                    caida != null && caida > UMBRAL_CAIDA_PORCENTAJE
        }

    // Sismo: está conectada, sin daño, pero vibra con amplitud muy alta
    val hasSismo: Boolean
        get() = connection == ConnectionStatus.CONNECTED && !hasCurrentAlert &&
                lectura != null && lectura.amplitud > AMPLITUD_SISMO
}

// Estructuras instaladas en terreno. La línea base se registra junto al sensor:
// la app la muestra, pero no la puede cambiar (lineamiento OT)
val ESTRUCTURAS_INSTALADAS = listOf(
    Estructura("NODO-01", "Edificio A", "Viga piso 2, eje C", 40.0, ConnectionStatus.CONNECTED),
    Estructura("NODO-02", "Edificio B", "Columna planta baja, eje A", 35.0, ConnectionStatus.CONNECTED),
    Estructura("NODO-03", "Bodega Norte", "Muro norte, nivel 1", 28.0, ConnectionStatus.CONNECTED)
)

// Arma la estructura con lo que se eligió en el Activador.
// Si el sensor está desconectado no hay lectura nueva (en Firebase se conserva la última)
fun armarEstructura(
    id: String,
    frecuenciaHz: Double,
    amplitud: Double,
    conectado: Boolean,
    nowMillis: Long
): Estructura? {
    val instalada = ESTRUCTURAS_INSTALADAS.find { it.id == id } ?: return null
    return instalada.copy(
        connection = if (conectado) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED,
        lectura = if (conectado) LecturaVibracion(frecuenciaHz, amplitud, nowMillis) else null
    )
}
