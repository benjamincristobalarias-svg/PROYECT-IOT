// Hecho con IA (Claude)
package cl.inacap.monitorestructural

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador del RecyclerView: dibuja cada estructura de la lista
 * usando el diseño item_estructura.xml.
 */
class EstructuraAdapter(private val lista: List<Estructura>) :
    RecyclerView.Adapter<EstructuraAdapter.Fila>() {

    /** Guarda las vistas de una fila para no buscarlas cada vez. */
    class Fila(vista: View) : RecyclerView.ViewHolder(vista) {
        val punto: View = vista.findViewById(R.id.punto)
        val tvNombre: TextView = vista.findViewById(R.id.tvNombre)
        val tvDetalle: TextView = vista.findViewById(R.id.tvDetalle)
        val tvEstado: TextView = vista.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Fila {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estructura, parent, false)
        return Fila(vista)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(fila: Fila, position: Int) {
        val estructura = lista[position]
        val contexto = fila.itemView.context
        val estado = Evaluador.estadoVisible(estructura.estado, estructura.actualizado)
        val color = ContextCompat.getColor(contexto, Evaluador.color(estado))

        fila.tvNombre.text = estructura.nombre
        fila.tvEstado.text = contexto.getString(Evaluador.texto(estado))
        fila.tvEstado.setTextColor(color)
        fila.punto.backgroundTintList = ColorStateList.valueOf(color)

        // Texto bajo el nombre, como en el mockup
        fila.tvDetalle.text = when (estado) {
            Evaluador.ALERTA -> contexto.getString(R.string.fila_caida, estructura.variacion)
            Evaluador.SIN_DATOS -> contexto.getString(R.string.fila_sin_datos)
            else -> contexto.getString(R.string.fila_frecuencia, estructura.frecuencia)
        }

        // Al tocar una fila se abre el detalle de esa estructura
        fila.itemView.setOnClickListener {
            val intent = Intent(contexto, DetalleActivity::class.java)
            intent.putExtra("id", estructura.id)
            contexto.startActivity(intent)
        }
    }
}
