// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Pantalla 3: lista de estructuras (celular de monitoreo).
 * Escucha /estructuras en Firebase: cada vez que el Activador envía
 * una medición, la lista se actualiza sola.
 */
class MonitoreoActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance()
    private val lista = ArrayList<Estructura>()
    private lateinit var adaptador: EstructuraAdapter
    private lateinit var tvSinConexion: TextView
    private lateinit var tvVacio: TextView
    private val manejador = Handler(Looper.getMainLooper())

    // Se ejecuta cada vez que cambia algo dentro de /estructuras
    private val escuchaEstructuras = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            lista.clear()
            for (dato in snapshot.children) {
                val estructura = Estructura.desdeFirebase(dato)
                if (estructura.estado.isNotEmpty()) {
                    lista.add(estructura)
                }
            }
            mostrarLista()
        }

        override fun onCancelled(error: DatabaseError) {
            // Por ejemplo, si las reglas de Firebase niegan la lectura
            Toast.makeText(this@MonitoreoActivity, error.message, Toast.LENGTH_LONG).show()
        }
    }

    // ".info/connected" es una ruta de Firebase que dice si el celular está conectado
    private val escuchaConexion = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val conectado = snapshot.getValue(Boolean::class.java) ?: false
            tvSinConexion.visibility = if (conectado) View.GONE else View.VISIBLE
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    // Cada 5 s se vuelve a dibujar la lista para detectar nodos que dejaron de enviar
    private val refresco = object : Runnable {
        override fun run() {
            mostrarLista()
            manejador.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_monitoreo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tvSinConexion = findViewById(R.id.tvSinConexion)
        tvVacio = findViewById(R.id.tvVacio)

        adaptador = EstructuraAdapter(lista)
        val rvEstructuras = findViewById<RecyclerView>(R.id.rvEstructuras)
        rvEstructuras.layoutManager = LinearLayoutManager(this)
        rvEstructuras.adapter = adaptador
    }

    // Se escucha a Firebase solo mientras la pantalla está visible (ahorra batería y datos)
    override fun onStart() {
        super.onStart()
        db.getReference("estructuras").addValueEventListener(escuchaEstructuras)
        db.getReference(".info/connected").addValueEventListener(escuchaConexion)
        manejador.post(refresco)
    }

    override fun onStop() {
        super.onStop()
        db.getReference("estructuras").removeEventListener(escuchaEstructuras)
        db.getReference(".info/connected").removeEventListener(escuchaConexion)
        manejador.removeCallbacks(refresco)
    }

    private fun mostrarLista() {
        // Lo más grave primero: rojo, amarillo, verde, azul, gris
        lista.sortBy { Evaluador.gravedad(Evaluador.estadoVisible(it.estado, it.actualizado)) }
        adaptador.notifyDataSetChanged()
        tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }
}
