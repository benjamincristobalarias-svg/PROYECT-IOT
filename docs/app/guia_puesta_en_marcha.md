# Guía: Firebase y ejecutar la app

> Hecho con IA (Claude).

## 1. Firebase (ya está hecho)

Proyecto **`monitor-estructural`** en console.firebase.google.com:

- **Realtime Database** en Estados Unidos (us-central1), con las reglas de `app-android/firebase/database.rules.json` publicadas.
- **Authentication** con **correo electrónico/contraseña** habilitado.
- App **Android** registrada con el paquete `cl.inacap.monitorestructural`.

Solo falta descargar **`google-services.json`**: ⚙️ **Configuración del proyecto → Tus apps → Monitor Estructural → google-services.json**.

Si hubiera que crear todo de nuevo: Crear proyecto (sin Analytics) → Realtime Database (Estados Unidos, modo de prueba) → pestaña Reglas: pegar `database.rules.json` y Publicar → Authentication → Comenzar → Correo electrónico/contraseña → Habilitar → Configuración del proyecto → Agregar app Android → descargar `google-services.json`.

## 2. Ejecutar la app

1. En GitHub: **Code → Download ZIP** y descomprimirlo.
2. Copiar `google-services.json` dentro de **`app-android/app/`**.
3. Android Studio → **Open** → carpeta **`app-android`** (no la raíz del repo) → esperar el **Gradle Sync**.
4. Elegir un emulador o un celular **con internet** y apretar **▶ Run**.
5. La primera vez: correo + contraseña (mínimo 8 caracteres) → **Crear cuenta**. Las cuentas quedan en Firebase → Authentication → Usuarios.

## 3. Dos celulares

- Dos equipos: un celular por USB + un emulador, o dos emuladores (Device Manager → crear otro; en el selector de dispositivos elegir **Select Multiple Devices** y luego Run).
- Iniciar sesión en los dos. Celular A → pestaña **Activador**; celular B → pestaña **Monitoreo**.

## 4. Qué mostrar en clase

| Acción | Qué se ve |
|---|---|
| Abrir la app sin sesión | Solo el Login |
| Contraseña mala | "Correo o contraseña incorrectos" y no entra |
| A: Edificio B, frecuencia 30 Hz, Enviar | B: Edificio B en rojo "Daño", caída 14,3 %. A: "Firebase confirmó en ... ms" |
| A: amplitud sobre 230.000 con frecuencia normal | B: amarillo "Vibración fuerte (sismo)" |
| A: apagar "Sensor conectado" y Enviar | B: "Desconectado" y lectura "no actual" |
| A en modo avión, enviar y volver a conectar | A: "Sin conexión"; al volver se confirma solo y llega a B |
| A: Envío automático | Las 3 estructuras cambian cada 3 s y crece `historial` en la consola |

Las pruebas de estabilidad se anotan en la tabla 11.3 del Informe Formativa U2.

## 5. Problemas comunes

| Problema | Qué revisar |
|---|---|
| `File google-services.json is missing` | Falta copiar el archivo en `app-android/app/` |
| "No se pudo conectar con Firebase" al crear cuenta | Que Authentication tenga habilitado correo/contraseña |
| "No se pudieron cargar los datos de Firebase" | Que haya sesión iniciada y que las reglas estén publicadas |
| "No se pudo enviar (¿sesión o permisos?)" | Que los valores estén en rango y que las reglas sean las del repo |
| Pide otro compileSdk o versión de AGP | Aceptar la actualización que indique Android Studio |
