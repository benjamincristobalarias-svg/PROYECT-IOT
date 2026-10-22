package cl.inacap.monitorestructural

import com.google.firebase.database.DataSnapshot

/**
 * Datos de una estructura, tal como se guardan en Firebase
 * (/estructuras/{id} y cada registro de /historial/{id}).
 */
data class Estructura(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val estado: String,
    val frecuencia: Double,
    val base: Double,
    val variacion: Double,
    val amplitud: Double,
    val actualizado: Long,   // hora del servidor en milisegundos
    val trama: String
) {
    companion object {

        /** Convierte un dato de Firebase en un objeto Estructura. */
        fun desdeFirebase(dato: DataSnapshot): Estructura {
            return Estructura(
                id = dato.key ?: "",
                nombre = texto(dato, "nombre"),
                ubicacion = texto(dato, "ubicacion"),
                estado = texto(dato, "estado"),
                frecuencia = numero(dato, "frecuencia"),
                base = numero(dato, "base"),
                variacion = numero(dato, "variacion"),
                amplitud = numero(dato, "amplitud"),
                actualizado = numero(dato, "actualizado").toLong(),
                trama = texto(dato, "trama")
            )
        }

        private fun texto(dato: DataSnapshot, campo: String): String {
            return dato.child(campo).value?.toString() ?: ""
        }

        // Firebase entrega los números como Long o como Double, según el valor
        private fun numero(dato: DataSnapshot, campo: String): Double {
            val valor = dato.child(campo).value
            return if (valor is Number) valor.toDouble() else 0.0
        }
    }
}
