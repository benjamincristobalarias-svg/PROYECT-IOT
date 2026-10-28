// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla 2: menú. Se elige qué hará este celular:
 * Monitoreo (ver las estructuras) o Activador (simular un nodo y enviar datos).
 */
class InicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        // Muestra con qué cuenta se inició sesión
        val usuario = FirebaseAuth.getInstance().currentUser
        findViewById<TextView>(R.id.tvUsuario).text = getString(R.string.sesion_de, usuario?.email)

        findViewById<MaterialCardView>(R.id.cardMonitoreo).setOnClickListener {
            startActivity(Intent(this, MonitoreoActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardActivador).setOnClickListener {
            startActivity(Intent(this, ActivadorActivity::class.java))
        }
        findViewById<Button>(R.id.btnSalir).setOnClickListener { cerrarSesion() }
    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        // Vuelve al login y borra las pantallas anteriores (no se puede volver con "atrás")
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
