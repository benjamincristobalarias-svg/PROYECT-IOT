# Guía: Firebase y ejecutar la app

> Hecho con IA (Claude).

## 1. Crear el proyecto en Firebase (una sola vez)

> **Ya está creado:** proyecto `monitor-estructural`, base en Estados Unidos (us-central1), reglas publicadas y app Android registrada. Solo falta descargar `google-services.json` desde ⚙️ **Configuración del proyecto → Tus apps → Monitor Estructural**. Los pasos de abajo quedan como referencia.

En **console.firebase.google.com**:

1. **Crear proyecto** → nombre `monitor-estructural` → desactivar Google Analytics → Crear.
2. **Compilación → Realtime Database** → Crear base de datos → **Estados Unidos** → **Comenzar en modo de prueba** → Habilitar.
3. Pestaña **Reglas**: pegar el contenido de `app-android/firebase/database.rules.json` y apretar **Publicar**.
4. ⚙️ **Configuración del proyecto** → Agregar app → **Android** → paquete `cl.inacap.monitorestructural` → Registrar.
5. Descargar **`google-services.json`**. Los demás pasos que muestra Firebase ya están en el código.

> Hacer el paso 2 antes que el 4: si no, `google-services.json` sale sin la dirección de la base de datos.

## 2. Abrir y ejecutar la app

1. En GitHub: **Code → Download ZIP** y descomprimirlo.
2. Copiar `google-services.json` dentro de **`app-android/app/`**.
3. Android Studio → **Open** → elegir la carpeta **`app-android`**, no la raíz del repo → esperar el **Gradle Sync**.
4. Elegir un emulador o un celular **con internet** y apretar **▶ Run**.

## 3. Qué mostrar en clase

Tener abiertas al mismo tiempo la app y la consola de Firebase (**Realtime Database → Datos**).

| Momento | En la app | En Firebase |
|---|---|---|
| Al abrir | Aparecen las 3 estructuras | Se crean `estructuras/NODO-01`, `NODO-02` y `NODO-03` |
| Cada 3 s | Cambian la frecuencia y la hora | Los valores se actualizan y crece `historial` |
| 6 s | Edificio B en rojo: "Daño", caída 14,3 % | `NODO-02/lectura/frecuenciaHz` = 30 |
| 12 s | Bodega Norte desconectada, con su lectura marcada como no actual | `NODO-03/conectada` = false |
| 15 s | Edificio A en amarillo: "Sismo" | `NODO-01/lectura/amplitud` = 260000 |
| Cambiar un dato a mano en la consola | La app muestra el cambio al tiro (hasta el siguiente envío) | — |

Para el informe: sacar capturas de la consola con `estructuras` y `historial` abiertos.

## 4. Problemas comunes

| Problema | Qué revisar |
|---|---|
| `File google-services.json is missing` | Falta copiar el archivo en `app-android/app/` |
| "No se pudieron cargar los datos de Firebase" | Que las reglas estén publicadas y que el celular tenga internet |
| La app no muestra nada y Firebase está vacío | Que la base se haya creado antes de descargar `google-services.json` |
| Pide otro compileSdk o versión de AGP | Aceptar la actualización que indique Android Studio |
