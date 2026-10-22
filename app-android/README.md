# App Android: Monitor estructural (Unidad 2, Evaluación 4)

App en Kotlin + Firebase para la sumativa de la Unidad 2. Dos celulares se comunican por WiFi a través de Firebase: uno hace de **Activador** (simula un nodo sensor) y el otro de **Monitoreo**.

> **Estado: en progreso.** Todavía no compila: faltan los archivos de la lista "Pendiente".

## Pantallas

1. **Login:** Firebase Authentication (correo y contraseña) y "¿Olvidaste tu contraseña?"
2. **Inicio:** elegir Monitoreo o Activador, y cerrar sesión
3. **Monitoreo:** lista de estructuras con colores (rojo, amarillo, verde y gris). Al tocar una se abre el detalle.
4. **Detalle:** estado actual, valores, última trama e historial
5. **Activador:** RadioButtons (nodo), Slider (frecuencia), Switch (sismo y envío automático) y botón Enviar

## Hecho

- Gradle: AGP 9.0.1, Gradle 9.1.0, Firebase BoM 35.0.2 y Material 1.13.0
- Código Kotlin: `Evaluador`, `Estructura`, `LoginActivity`, `InicioActivity`, `MonitoreoActivity`, `EstructuraAdapter`, `DetalleActivity` y `ActivadorActivity`
- `strings.xml`, `colors.xml`, `themes.xml`, `AndroidManifest.xml` y `network_security_config.xml` (solo HTTPS/TLS)
- Layouts: `activity_login`, `activity_inicio`, `activity_monitoreo` e `item_estructura`

## Pendiente

- Layouts `activity_detalle.xml` y `activity_activador.xml`
- Drawables `punto`, `ic_logo`, `ic_monitoreo`, `ic_activador` e `ic_launcher_foreground`, y mipmaps `ic_launcher` / `ic_launcher_round`
- Verificar que compile
- Reglas de Firebase (solo usuarios con sesión) y guía para crear el proyecto en Firebase
- Documento de seguridad (ISO 27400 / OT) y plan de pruebas

## Criterio acordado

Mantenerlo simple y dentro de lo que pide la guía del profe: `findViewById`, código de la plantilla de Android Studio y sin extras (nada de roles, cifrado adicional, notificaciones ni gráficos).

`app/google-services.json` no se sube al repo: se descarga desde Firebase.
