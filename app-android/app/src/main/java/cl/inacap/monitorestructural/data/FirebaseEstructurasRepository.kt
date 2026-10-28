// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.data

import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.model.LecturaVibracion
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Fuente de datos real: Firebase Realtime Database.
// Cumple el mismo contrato que el simulador, así el ViewModel y la pantalla no cambian.
//
// Datos en Firebase:
//   estructuras/{id}         -> último estado de cada estructura
//   historial/{id}/{clave}   -> cada lectura enviada (registro de todo lo que llegó)
class FirebaseEstructurasRepository : EstructurasRepository {

    private val database = FirebaseDatabase.getInstance()
    private val estructuras = database.getReference("estructuras")
    private val historial = database.getReference("historial")

    // LEER: cada vez que cambia algo en "estructuras", Firebase avisa y se emite la lista nueva
    override fun observeEstructuras(): Flow<List<Estructura>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { leerEstructura(it) }
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                // Por ejemplo, si las reglas no permiten leer: el ViewModel muestra el error
                close(error.toException())
            }
        }
        estructuras.addValueEventListener(listener)
        // Cuando nadie observa (pantalla cerrada), se deja de escuchar a Firebase
        awaitClose { estructuras.removeEventListener(listener) }
    }

    // ENVIAR: guarda el estado actual de cada estructura y agrega la lectura al historial
    fun guardar(lista: List<Estructura>) {
        for (estructura in lista) {
            estructuras.child(estructura.id).setValue(aMapa(estructura))

            val lectura = estructura.lectura
            if (estructura.connection == ConnectionStatus.CONNECTED && lectura != null) {
                // push() crea un registro nuevo con una clave única
                historial.child(estructura.id).push().setValue(lecturaAMapa(lectura))
            }
        }
    }

    // Estructura -> datos que entiende Firebase
    private fun aMapa(estructura: Estructura): Map<String, Any?> = mapOf(
        "nombre" to estructura.nombre,
        "ubicacion" to estructura.ubicacion,
        "frecuenciaBaseHz" to estructura.frecuenciaBaseHz,
        "conectada" to (estructura.connection == ConnectionStatus.CONNECTED),
        "lectura" to if (estructura.lectura == null) null else lecturaAMapa(estructura.lectura)
    )

    private fun lecturaAMapa(lectura: LecturaVibracion): Map<String, Any> = mapOf(
        "frecuenciaHz" to lectura.frecuenciaHz,
        "amplitud" to lectura.amplitud,
        "measuredAtMillis" to lectura.measuredAtMillis
    )

    // Datos de Firebase -> Estructura. Si falta el nombre, el registro se descarta
    private fun leerEstructura(dato: DataSnapshot): Estructura? {
        val id = dato.key ?: return null
        val nombre = dato.child("nombre").value as? String ?: return null

        val datoLectura = dato.child("lectura")
        val lectura = if (datoLectura.exists()) {
            LecturaVibracion(
                frecuenciaHz = numero(datoLectura, "frecuenciaHz"),
                amplitud = numero(datoLectura, "amplitud"),
                measuredAtMillis = numero(datoLectura, "measuredAtMillis").toLong()
            )
        } else {
            null    // sin datos: null, nunca un 0 inventado
        }

        return Estructura(
            id = id,
            nombre = nombre,
            ubicacion = dato.child("ubicacion").value as? String ?: "",
            frecuenciaBaseHz = numero(dato, "frecuenciaBaseHz"),
            connection = if (dato.child("conectada").value == true)
                ConnectionStatus.CONNECTED
            else ConnectionStatus.DISCONNECTED,
            lectura = lectura
        )
    }

    // Firebase entrega los números como Long o como Double según el valor
    private fun numero(dato: DataSnapshot, campo: String): Double {
        val valor = dato.child(campo).value
        return if (valor is Number) valor.toDouble() else 0.0
    }
}
