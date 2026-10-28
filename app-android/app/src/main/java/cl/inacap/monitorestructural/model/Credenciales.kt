// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.model

// Revisa el correo y la contraseña antes de enviarlos a Firebase.
// Devuelve el mensaje de error, o null si están bien.
fun validarCredenciales(correo: String, clave: String, cuentaNueva: Boolean): String? = when {
    !CORREO_VALIDO.matches(correo.trim()) -> "Escribe un correo válido."
    clave.isEmpty() -> "Escribe la contraseña."
    cuentaNueva && clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
    else -> null
}

private val CORREO_VALIDO = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
