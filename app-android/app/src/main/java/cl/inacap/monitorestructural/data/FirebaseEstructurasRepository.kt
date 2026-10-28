// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.data

import cl.inacap.monitorestructural.model.ConnectionStatus
import cl.inacap.monitorestructural.model.Estructura
import cl.inacap.monitorestructural.model.LecturaVibracion
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Fuente de datos real: Firebase Realtime Database.
// Cumple el mismo contrato que el simulador, así la pantalla de Monitoreo no cambia.
//
// Datos en Firebase (las reglas exigen haber iniciado sesión):
//   estructuras/{id}         -> último estado de cada estructura
//   historial/{id}/{clave}   -> cada lectura enviada y quién la envió (solo se agrega, no se borra)
class FirebaseEstructurasRepository : EstructurasRepository {

    private val database = FirebaseDatabase.getInstance()
    private val estructuras = database.getReference("estructuras")
    private val historial = database.getReference("historial")
    private val auth = FirebaseAuth.getInstance()

    // LEER (Monitoreo): cada vez que cambia algo en "estructuras", Firebase avisa y se emite la lista nueva
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

    // CONEXIÓN: ".info/connected" vale true mientras el celular está conectado a Firebase
    fun observeConexion(): Flow<Boolean> = callbackFlow {
        val conexion = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.value == true)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        conexion.addValueEventListener(listener)
        awaitClose { conexion.removeEventListener(listener) }
    }

    // ENVIAR (Activador): actualiza la estructura y agrega la lectura al historial.
    // Devuelve la tarea de Firebase para saber cuándo el servidor confirmó el envío.
    fun enviar(estructura: Estructura): Task<Void> {
        val datos = mutableMapOf<String, Any>(
            "nombre" to estructura.nombre,
            "ubicacion" to estructura.ubicacion,
            "frecuenciaBaseHz" to estructura.frecuenciaBaseHz,
            "conectada" to (estructura.connection == ConnectionStatus.CONNECTED)
        )
        val lectura = estructura.lectura
        if (estructura.connection == ConnectionStatus.CONNECTED && lectura != null) {
            datos["lectura"] = lecturaAMapa(lectura)
            // historial: la lectura y el usuario que la envió
            // (las reglas revisan que "usuario" sea el mismo que inició sesión)
            historial.child(estructura.id).push()
                .setValue(lecturaAMapa(lectura) + ("usuario" to (auth.currentUser?.uid ?: "")))
        }
        // updateChildren cambia solo estos campos: si el sensor se desconecta,
        // en Firebase se conserva su última lectura
        return estructuras.child(estructura.id).updateChildren(datos)
    }

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
