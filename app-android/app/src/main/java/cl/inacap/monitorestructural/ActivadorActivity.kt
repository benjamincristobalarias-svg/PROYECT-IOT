// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla 5: Activador. Este celular hace de nodo sensor:
 * - RadioButtons: qué estructura (nodo) se simula.
 * - Slider: la frecuencia medida (bajarla simula pérdida de rigidez).
 * - Switch "sismo": sube la amplitud de la vibración.
 * - Botón Enviar (o envío automático cada 3 s): guarda la medición en Firebase
 *   y el celular de monitoreo la recibe al instante.
 */
class ActivadorActivity : AppCompatActivity() {

    /** Datos fijos de cada nodo que se puede simular. */
    data class Nodo(val id: String, val nombre: String, val ubicacion: String)

    private val db = FirebaseDatabase.getInstance()
    private val manejador = Handler(Looper.getMainLooper())

    private lateinit var rgNodo: RadioGroup
    private lateinit var sliderFrecuencia: Slider
    private lateinit var swSismo: MaterialSwitch
    private lateinit var swAutomatico: MaterialSwitch
    private lateinit var tvFrecuencia: TextView
    private lateinit var tvBase: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvTrama: TextView
    private lateinit var tvEnvio: TextView

    // Envía una medición cada 3 segundos mientras el switch está activo
    private val envioAutomatico = object : Runnable {
        override fun run() {
            enviar()
            manejador.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activador)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        rgNodo = findViewById(R.id.rgNodo)
        sliderFrecuencia = findViewById(R.id.sliderFrecuencia)
        swSismo = findViewById(R.id.swSismo)
        swAutomatico = findViewById(R.id.swAutomatico)
        tvFrecuencia = findViewById(R.id.tvFrecuencia)
        tvBase = findViewById(R.id.tvBase)
        tvEstado = findViewById(R.id.tvEstado)
        tvTrama = findViewById(R.id.tvTrama)
        tvEnvio = findViewById(R.id.tvEnvio)

        // Cada cambio en los controles actualiza la vista previa
        rgNodo.setOnCheckedChangeListener { _, _ -> actualizarVista() }
        sliderFrecuencia.addOnChangeListener { _, _, _ -> actualizarVista() }
        swSismo.setOnCheckedChangeListener { _, _ -> actualizarVista() }

        swAutomatico.setOnCheckedChangeListener { _, activo ->
            manejador.removeCallbacks(envioAutomatico)
            if (activo) {
                manejador.post(envioAutomatico)
            }
        }

        findViewById<Button>(R.id.btnBase).setOnClickListener { registrarBase() }
        findViewById<Button>(R.id.btnEnviar).setOnClickListener { enviar() }

        actualizarVista()
    }

    override fun onStop() {
        super.onStop()
        // Si se sale de la pantalla, se detiene el envío automático
        swAutomatico.isChecked = false
    }

    /** Nodo elegido en los RadioButtons. */
    private fun nodoSeleccionado(): Nodo {
        return when (rgNodo.checkedRadioButtonId) {
            R.id.rbNodo2 -> Nodo("NODO-02", "Edificio B", "Columna planta baja, eje A")
            R.id.rbNodo3 -> Nodo("NODO-03", "Bodega Norte", "Muro norte, nivel 1")
            else -> Nodo("NODO-01", "Edificio A", "Viga piso 2, eje C")
        }
    }

    /** Frecuencia del slider, con un decimal. */
    private fun frecuenciaActual(): Double {
        return Math.round(sliderFrecuencia.value * 10) / 10.0
    }

    /** Amplitud de la vibración: alta si el switch de sismo está activo. */
    private fun amplitudActual(): Double {
        return if (swSismo.isChecked) 260000.0 else 150000.0
    }

    /** Línea base guardada en el celular (como la EEPROM del Arduino). 0 = no registrada. */
    private fun leerBase(nodo: Nodo): Double {
        val memoria = getSharedPreferences("activador", MODE_PRIVATE)
        val base = memoria.getFloat("base_" + nodo.id, 0f)
        return Math.round(base * 10) / 10.0
    }

    /** Guarda la frecuencia actual como línea base de la estructura sana. */
    private fun registrarBase() {
        val nodo = nodoSeleccionado()
        val frecuencia = frecuenciaActual()
        val memoria = getSharedPreferences("activador", MODE_PRIVATE)
        memoria.edit().putFloat("base_" + nodo.id, frecuencia.toFloat()).apply()

        Toast.makeText(this, getString(R.string.base_registrada, frecuencia), Toast.LENGTH_SHORT).show()
        actualizarVista()
        enviar()
    }

    /** Muestra la frecuencia, la línea base y el estado que se enviaría. */
    private fun actualizarVista() {
        val nodo = nodoSeleccionado()
        val frecuencia = frecuenciaActual()
        val base = leerBase(nodo)
        val amplitud = amplitudActual()
        val estado = Evaluador.evaluar(frecuencia, base, amplitud)
        val variacion = Evaluador.variacion(frecuencia, base)

        tvFrecuencia.text = getString(R.string.valor_hz, frecuencia)
        tvBase.text = if (base > 0) {
            getString(R.string.base_actual, base)
        } else {
            getString(R.string.base_no_registrada)
        }
        tvEstado.text = getString(Evaluador.texto(estado))
        tvEstado.setTextColor(ContextCompat.getColor(this, Evaluador.color(estado)))
        tvTrama.text = Evaluador.armarTrama(nodo.id, estado, frecuencia, base, variacion, amplitud)
    }

    /** Guarda la medición en Firebase. El celular de monitoreo la recibe al instante. */
    private fun enviar() {
        val nodo = nodoSeleccionado()
        val frecuencia = frecuenciaActual()
        val base = leerBase(nodo)
        val amplitud = amplitudActual()
        val estado = Evaluador.evaluar(frecuencia, base, amplitud)
        val variacion = Math.round(Evaluador.variacion(frecuencia, base) * 10) / 10.0
        val trama = Evaluador.armarTrama(nodo.id, estado, frecuencia, base, variacion, amplitud)

        val datos = HashMap<String, Any>()
        datos["nombre"] = nodo.nombre
        datos["ubicacion"] = nodo.ubicacion
        datos["estado"] = estado
        datos["frecuencia"] = frecuencia
        datos["base"] = base
        datos["variacion"] = variacion
        datos["amplitud"] = amplitud
        datos["trama"] = trama
        datos["actualizado"] = ServerValue.TIMESTAMP   // hora del servidor de Firebase

        tvEnvio.text = getString(R.string.enviando)

        // 1) Estado actual: se sobrescribe en /estructuras/{id}
        db.getReference("estructuras").child(nodo.id).setValue(datos)
            .addOnSuccessListener {
                tvEnvio.text = getString(R.string.envio_ok, nodo.id, horaActual())
            }
            .addOnFailureListener { error ->
                tvEnvio.text = getString(R.string.envio_error, error.message)
            }

        // 2) Historial: push() crea un registro nuevo con un ID único
        db.getReference("historial").child(nodo.id).push().setValue(datos)
    }

    private fun horaActual(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
