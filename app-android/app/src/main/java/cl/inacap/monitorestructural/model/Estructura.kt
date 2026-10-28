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
