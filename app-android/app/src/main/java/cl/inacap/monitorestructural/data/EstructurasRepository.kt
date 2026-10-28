// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.data

import cl.inacap.monitorestructural.model.Estructura
import kotlinx.coroutines.flow.Flow

// Contrato de la fuente de datos: hoy la cumple el simulador,
// más adelante la podrá cumplir Firebase o el sensor real.
interface EstructurasRepository {
    fun observeEstructuras(): Flow<List<Estructura>>
}
