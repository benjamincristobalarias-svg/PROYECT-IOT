// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// Inicio de sesión con Firebase Authentication (correo y contraseña).
// La app no guarda la contraseña: Firebase la guarda cifrada (hash) en sus servidores
// y la envía por una conexión cifrada (HTTPS/TLS).
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    // Correo del usuario con sesión, o null si nadie ha iniciado sesión
    fun usuarioActual(): String? = auth.currentUser?.email

    fun ingresar(correo: String, clave: String, alTerminar: (usuario: String?, error: String?) -> Unit) {
        auth.signInWithEmailAndPassword(correo, clave)
            .addOnCompleteListener { tarea -> responder(tarea, alTerminar) }
    }

    fun crearCuenta(correo: String, clave: String, alTerminar: (usuario: String?, error: String?) -> Unit) {
        auth.createUserWithEmailAndPassword(correo, clave)
            .addOnCompleteListener { tarea -> responder(tarea, alTerminar) }
    }

    fun salir() = auth.signOut()

    private fun responder(tarea: Task<AuthResult>, alTerminar: (String?, String?) -> Unit) {
        if (tarea.isSuccessful) {
            alTerminar(auth.currentUser?.email, null)
        } else {
            alTerminar(null, mensajeError(tarea.exception))
        }
    }

    // Si falla el correo o la contraseña se muestra el mismo mensaje,
    // así no se revela si el correo tiene cuenta (buena práctica de seguridad)
    private fun mensajeError(error: Exception?): String = when (error) {
        is FirebaseNetworkException -> "Sin conexión a internet. Intenta de nuevo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
        is FirebaseAuthUserCollisionException -> "Ese correo ya tiene una cuenta."
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Correo o contraseña incorrectos."
        else -> "No se pudo conectar con Firebase. Intenta de nuevo."
    }
}
