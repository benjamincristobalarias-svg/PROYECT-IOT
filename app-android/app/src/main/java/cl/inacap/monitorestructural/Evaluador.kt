package cl.inacap.monitorestructural

import java.util.Locale

/**
 * Reglas de estado del proyecto. Son las mismas del Arduino / ESP32
 * de la Unidad 1, así el Activador entrega lo mismo que un nodo real.
 */
object Evaluador {

    const val UMBRAL_CAIDA = 0.05          // si la frecuencia baja más de 5 % -> daño
    const val AMPLITUD_SISMO = 230000.0    // sobre esta amplitud -> sismo
    const val TIEMPO_SIN_DATOS = 15000L    // 15 s sin datos -> nodo desconectado

    // Estados (el mismo texto que viaja en la trama)
    const val ALERTA = "ALERTA"         // rojo: pérdida de rigidez
    const val VIBRACION = "VIBRACION"   // amarillo: sismo sin daño
    const val NORMAL = "NORMAL"         // verde: estructura sana
    const val SIN_BASE = "SIN_BASE"     // azul: falta registrar la línea base
    const val SIN_DATOS = "SIN_DATOS"   // gris: el nodo dejó de enviar (lo decide la app)

    /** Estado según la frecuencia, la línea base y la amplitud. Prioridad: rojo > amarillo > verde. */
    fun evaluar(frecuencia: Double, base: Double, amplitud: Double): String {
        if (base <= 0) return SIN_BASE
        val caida = (base - frecuencia) / base
        return when {
            caida > UMBRAL_CAIDA -> ALERTA
            amplitud > AMPLITUD_SISMO -> VIBRACION
            else -> NORMAL
        }
    }

    /** Porcentaje que bajó la frecuencia respecto de la línea base. */
    fun variacion(frecuencia: Double, base: Double): Double {
        if (base <= 0) return 0.0
        return (base - frecuencia) / base * 100
    }

    /** Trama igual a la del nodo físico: ID;ESTADO;frecuencia;linea_base;variacion;amplitud */
    fun armarTrama(
        id: String, estado: String, frecuencia: Double,
        base: Double, variacion: Double, amplitud: Double
    ): String {
        // Locale.US para que los decimales vayan con punto, como en el Arduino
        return String.format(
            Locale.US, "%s;%s;%.2f;%.2f;%.1f;%.0f",
            id, estado, frecuencia, base, variacion, amplitud
        )
    }

    /** Si el último dato tiene más de 15 s, el nodo se muestra "sin datos". */
    fun estadoVisible(estado: String, actualizado: Long): String {
        val antiguedad = System.currentTimeMillis() - actualizado
        return if (antiguedad > TIEMPO_SIN_DATOS) SIN_DATOS else estado
    }

    /** Para ordenar la lista: lo más grave queda arriba. */
    fun gravedad(estado: String): Int {
        return when (estado) {
            ALERTA -> 0
            VIBRACION -> 1
            NORMAL -> 2
            SIN_BASE -> 3
            else -> 4
        }
    }

    /** Color fuerte de cada estado (puntos y textos). */
    fun color(estado: String): Int {
        return when (estado) {
            ALERTA -> R.color.rojo
            VIBRACION -> R.color.amarillo
            NORMAL -> R.color.verde
            SIN_BASE -> R.color.azul
            else -> R.color.gris
        }
    }

    /** Color suave de cada estado (fondo del aviso en el detalle). */
    fun colorClaro(estado: String): Int {
        return when (estado) {
            ALERTA -> R.color.rojo_claro
            VIBRACION -> R.color.amarillo_claro
            NORMAL -> R.color.verde_claro
            SIN_BASE -> R.color.azul_claro
            else -> R.color.gris_claro
        }
    }

    /** Nombre corto del estado para mostrar en pantalla. */
    fun texto(estado: String): Int {
        return when (estado) {
            ALERTA -> R.string.estado_alerta
            VIBRACION -> R.string.estado_vibracion
            NORMAL -> R.string.estado_normal
            SIN_BASE -> R.string.estado_sin_base
            else -> R.string.estado_sin_datos
        }
    }
}
