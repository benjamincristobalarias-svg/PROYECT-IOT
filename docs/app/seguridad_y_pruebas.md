# Seguridad y pruebas de la app

> Hecho con IA (Claude).

## Medidas de seguridad (ISO/IEC 27001, Anexo A)

| Medida | Cómo está en la app | Control |
|---|---|---|
| Control de acceso | Login obligatorio y reglas `auth != null` | 5.15, 8.3 |
| Autenticación segura | Firebase Authentication; contraseña oculta; mínimo 8 caracteres al crear cuenta; mismo mensaje si falla el correo o la contraseña | 8.5 |
| Credenciales protegidas | La app no guarda la contraseña; Firebase la guarda cifrada (hash) | 5.17 |
| Cifrado en tránsito | HTTPS/TLS entre la app y Firebase | 8.24 |
| Integridad de los datos | Reglas que validan tipos y rangos (0–100 Hz) y rechazan campos desconocidos | 8.26 |
| Registro de eventos | `historial` guarda cada lectura con el UID de quien la envió; no se puede modificar ni borrar desde la app | 8.15 |
| Mínimo privilegio | Solo el permiso INTERNET y solo dos ramas de la base; `google-services.json` fuera del repo | 8.3 |

## Lineamientos OT (base: IEC 62443)

- **Identificar usuarios:** login obligatorio.
- **Control de uso:** solo dos ramas; historial que no se modifica; la línea base no se cambia desde la app (se registra en terreno).
- **Integridad:** valores fuera de rango rechazados por las reglas.
- **Disponibilidad:** indicador "Conectado a Firebase / Sin conexión"; sin internet Firebase guarda los envíos y los manda al volver.
- **Conexión y condición separadas:** un sensor desconectado no se muestra como "Normal"; su lectura queda como "no actual".
- **Sin datos inventados:** si no hay lectura se guarda `null` y se muestra "Sin datos".

## Pruebas

- **11 pruebas automáticas** (`SimulacionTest.kt`, `LoginYActivadorTest.kt`): pasaron todas.
- **6 pruebas de reglas** en la Zona de pruebas de reglas de Firebase: lectura sin sesión rechazada, lectura con sesión autorizada, 500 Hz rechazado, 30 Hz autorizado, historial a nombre de otro usuario rechazado, historial a nombre propio autorizado.
- **Pruebas con dos celulares** (WiFi, datos móviles, sin internet, envío automático, sensor desconectado, contraseña incorrecta): tabla 11.3 del Informe Formativa U2.

## Pendiente

- Reemplazar el Activador por el ESP32 enviando por WiFi (el ESP32 también tendrá que iniciar sesión).
- En producción: desactivar el registro libre y que el administrador cree las cuentas.
