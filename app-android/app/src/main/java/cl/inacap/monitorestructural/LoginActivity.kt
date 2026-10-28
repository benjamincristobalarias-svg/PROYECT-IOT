// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla 1: inicio de sesión con Firebase Authentication (correo y contraseña).
 * La contraseña viaja cifrada (HTTPS/TLS) y nunca se guarda en el celular.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etClave: TextInputEditText
    private lateinit var btnIngresar: Button
    private lateinit var progreso: ProgressBar
    private lateinit var tvMensaje: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Si ya inició sesión antes, pasa directo al menú
        if (auth.currentUser != null) {
            abrirInicio()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // ime() = teclado: así el teclado no tapa el botón Ingresar
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        etCorreo = findViewById(R.id.etCorreo)
        etClave = findViewById(R.id.etClave)
        btnIngresar = findViewById(R.id.btnIngresar)
        progreso = findViewById(R.id.progreso)
        tvMensaje = findViewById(R.id.tvMensaje)

        btnIngresar.setOnClickListener { iniciarSesion() }
        findViewById<Button>(R.id.btnOlvide).setOnClickListener { recuperarClave() }
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val clave = etClave.text.toString()

        // Validaciones antes de llamar a Firebase
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarMensaje(getString(R.string.error_correo))
            return
        }
        if (clave.isEmpty()) {
            mostrarMensaje(getString(R.string.error_clave_vacia))
            return
        }

        tvMensaje.visibility = View.GONE
        progreso.visibility = View.VISIBLE
        btnIngresar.isEnabled = false

        auth.signInWithEmailAndPassword(correo, clave)
            .addOnCompleteListener(this) { tarea ->
                progreso.visibility = View.GONE
                btnIngresar.isEnabled = true

                if (tarea.isSuccessful) {
                    abrirInicio()
                } else if (tarea.exception is FirebaseNetworkException) {
                    mostrarMensaje(getString(R.string.error_red))
                } else {
                    // El mismo mensaje si falla el correo o la contraseña:
                    // así no se revela qué cuentas existen
                    mostrarMensaje(getString(R.string.error_credenciales))
                    etClave.setText("")
                }
            }
    }

    /** Firebase envía un correo para crear una contraseña nueva. */
    private fun recuperarClave() {
        val correo = etCorreo.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarMensaje(getString(R.string.error_correo_recuperar))
            return
        }
        auth.sendPasswordResetEmail(correo)
        Toast.makeText(this, R.string.recuperar_enviado, Toast.LENGTH_LONG).show()
    }

    private fun mostrarMensaje(texto: String) {
        tvMensaje.text = texto
        tvMensaje.visibility = View.VISIBLE
    }

    private fun abrirInicio() {
        startActivity(Intent(this, InicioActivity::class.java))
        finish()
    }
}
