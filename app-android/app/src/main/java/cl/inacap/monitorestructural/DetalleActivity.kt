// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla 4: detalle de una estructura. Recibe el ID desde la lista
 * (Intent extra "id") y lee de Firebase su estado actual y su historial.
 */
class DetalleActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance()
    private val manejador = Handler(Looper.getMainLooper())
    private var ultima: Estructura? = null

    private lateinit var refEstructura: Query
    private lateinit var refHistorial: Query

    private lateinit var tvNombre: TextView
    private lateinit var tvUbicacion: TextView
    private lateinit var cardEstado: MaterialCardView
    private lateinit var tvEstadoTitulo: TextView
    private lateinit var tvEstadoDescripcion: TextView
    private lateinit var tvActual: TextView
    private lateinit var tvBase: TextView
    private lateinit var tvVariacion: TextView
    private lateinit var tvAmplitud: TextView
    private lateinit var tvTrama: TextView
    private lateinit var tvActualizado: TextView
    private lateinit var tvHistorial: TextView

    // Estado actual: /estructuras/{id}
    private val escuchaEstructura = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                ultima = Estructura.desdeFirebase(snapshot)
                mostrar()
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    // Últimas 10 mediciones: /historial/{id}
    private val escuchaHistorial = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val lineas = ArrayList<String>()
            for (dato in snapshot.children) {
                val registro = Estructura.desdeFirebase(dato)
                val linea = getString(
                    R.string.linea_historial,
                    hora(registro.actualizado),
                    getString(Evaluador.texto(registro.estado)),
                    registro.frecuencia
                )
                lineas.add(0, linea)   // la más nueva queda arriba
            }
            tvHistorial.text = if (lineas.isEmpty()) {
                getString(R.string.sin_historial)
            } else {
                lineas.joinToString("\n")
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    // Cada 5 s se revisa si el nodo dejó de enviar (pasa a gris)
    private val refresco = object : Runnable {
        override fun run() {
            mostrar()
            manejador.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tvNombre = findViewById(R.id.tvNombre)
        tvUbicacion = findViewById(R.id.tvUbicacion)
        cardEstado = findViewById(R.id.cardEstado)
        tvEstadoTitulo = findViewById(R.id.tvEstadoTitulo)
        tvEstadoDescripcion = findViewById(R.id.tvEstadoDescripcion)
        tvActual = findViewById(R.id.tvActual)
        tvBase = findViewById(R.id.tvBase)
        tvVariacion = findViewById(R.id.tvVariacion)
        tvAmplitud = findViewById(R.id.tvAmplitud)
        tvTrama = findViewById(R.id.tvTrama)
        tvActualizado = findViewById(R.id.tvActualizado)
        tvHistorial = findViewById(R.id.tvHistorial)

        // ID de la estructura que se tocó en la lista
        val id = intent.getStringExtra("id") ?: ""
        tvNombre.text = id
        refEstructura = db.getReference("estructuras").child(id)
        refHistorial = db.getReference("historial").child(id).limitToLast(10)
    }

    override fun onStart() {
        super.onStart()
        refEstructura.addValueEventListener(escuchaEstructura)
        refHistorial.addValueEventListener(escuchaHistorial)
        manejador.post(refresco)
    }

    override fun onStop() {
        super.onStop()
        refEstructura.removeEventListener(escuchaEstructura)
        refHistorial.removeEventListener(escuchaHistorial)
        manejador.removeCallbacks(refresco)
    }

    private fun mostrar() {
        val e = ultima ?: return
        val estado = Evaluador.estadoVisible(e.estado, e.actualizado)

        tvNombre.text = e.nombre
        tvUbicacion.text = getString(R.string.ubicacion_y_id, e.ubicacion, e.id)

        // Aviso de color según el estado
        cardEstado.setCardBackgroundColor(ContextCompat.getColor(this, Evaluador.colorClaro(estado)))
        tvEstadoTitulo.setTextColor(ContextCompat.getColor(this, Evaluador.color(estado)))
        when (estado) {
            Evaluador.ALERTA -> mostrarAviso(R.string.aviso_alerta, R.string.aviso_alerta_desc)
            Evaluador.VIBRACION -> mostrarAviso(R.string.aviso_vibracion, R.string.aviso_vibracion_desc)
            Evaluador.NORMAL -> mostrarAviso(R.string.aviso_normal, R.string.aviso_normal_desc)
            Evaluador.SIN_BASE -> mostrarAviso(R.string.aviso_sin_base, R.string.aviso_sin_base_desc)
            else -> mostrarAviso(R.string.aviso_sin_datos, R.string.aviso_sin_datos_desc)
        }

        tvActual.text = getString(R.string.valor_hz, e.frecuencia)
        tvBase.text = getString(R.string.valor_hz, e.base)
        tvVariacion.text = getString(R.string.valor_porcentaje, e.variacion)
        tvAmplitud.text = getString(R.string.valor_entero, e.amplitud)
        tvTrama.text = e.trama
        tvActualizado.text = getString(R.string.recibido_a_las, hora(e.actualizado))
    }

    private fun mostrarAviso(titulo: Int, descripcion: Int) {
        tvEstadoTitulo.setText(titulo)
        tvEstadoDescripcion.setText(descripcion)
    }

    private fun hora(milisegundos: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(milisegundos))
    }
}
