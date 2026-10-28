# App Android: Monitor estructural (Unidad 2, Evaluación 4)

> Hecho con IA (Claude).

App en Kotlin + Firebase. Dos celulares se comunican por WiFi o datos móviles a través de Firebase. Uno hace de **Activador** (simula un nodo sensor) y el otro de **Monitoreo**.

- Puesta en marcha y guion de la demo: [`docs/app/guia_puesta_en_marcha.md`](../docs/app/guia_puesta_en_marcha.md)
- Seguridad (ISO 27400 / OT) y plan de pruebas: [`docs/app/seguridad_y_pruebas.md`](../docs/app/seguridad_y_pruebas.md)

## Pantallas

1. **Login:** Firebase Authentication (correo y contraseña) y "¿Olvidaste tu contraseña?"
2. **Inicio:** elegir Monitoreo o Activador, y cerrar sesión
3. **Monitoreo:** lista de estructuras con colores (rojo, amarillo, verde, azul y gris), con lo más grave arriba
4. **Detalle:** estado, valores, última trama e historial de las últimas 10 mediciones
5. **Activador:** RadioButtons (nodo), Slider (frecuencia), Switches (sismo y envío automático) y botón Enviar

## Qué hace cada archivo

| Archivo | Qué hace |
|---|---|
| `Evaluador.kt` | Reglas de estado de la Unidad 1 (caída > 5 % = daño, amplitud alta = sismo), arma la trama y decide colores y textos |
| `Estructura.kt` | Clase de datos y conversión desde Firebase (`DataSnapshot`) |
| `LoginActivity.kt` | Inicio de sesión con `signInWithEmailAndPassword` y recuperación de contraseña |
| `InicioActivity.kt` | Menú: abre Monitoreo o Activador y cierra sesión |
| `MonitoreoActivity.kt` | Escucha `/estructuras` con `addValueEventListener`, ordena la lista y muestra el aviso "Sin conexión" |
| `EstructuraAdapter.kt` | Adaptador del RecyclerView: dibuja cada fila y abre el detalle con un `Intent` |
| `DetalleActivity.kt` | Lee `/estructuras/{id}` y `/historial/{id}` (últimas 10) |
| `ActivadorActivity.kt` | Simula el nodo: calcula el estado y guarda con `setValue` en `/estructuras` y `push()` en `/historial` |
| `res/layout/*.xml` | Diseño de las pantallas (Material Design 3) |
| `res/xml/network_security_config.xml` | Solo permite conexiones cifradas (HTTPS/TLS) |
| `firebase/database.rules.json` | Reglas: solo usuarios con sesión leen y escriben |

## Datos en Firebase

```
estructuras/NODO-01  -> último estado (nombre, estado, frecuencia, base, variacion, amplitud, trama, actualizado)
historial/NODO-01/{id} -> cada medición enviada (se agrega con push)
```

## Versiones

AGP 9.0.1 · Gradle 9.1.0 · compileSdk 36 · minSdk 26 (Android 8.0) · Material 1.13.0 · Firebase BoM 35.0.2

`app/google-services.json` no se sube al repo: se descarga desde la consola de Firebase y se copia en `app-android/app/`.

## Estado

Código completo. Se revisó que compile sin errores contra Android 36: los ids y tipos de todas las vistas coinciden con los layouts y las reglas de estado dan lo esperado. **Falta probarla en los celulares con Android Studio.**
