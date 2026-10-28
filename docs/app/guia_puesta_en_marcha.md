# Guía: poner la app en marcha

> Hecho con IA (Claude).

## 1. Firebase (se hace una sola vez)

En **console.firebase.google.com**:

1. **Crear proyecto**: nombre `monitor-estructural`, desactiva Google Analytics y crea.
2. **Authentication**: Comenzar → **Correo electrónico/contraseña** → Habilitar → Guardar.
3. Authentication → **Usuarios** → Agregar usuario: uno para cada integrante (correo y contraseña).
4. **Realtime Database**: Crear base de datos → **Estados Unidos** → **modo bloqueado**.
5. Pestaña **Reglas**: pega el contenido de `app-android/firebase/database.rules.json` y aprieta **Publicar**:
   ```json
   {
     "rules": {
       ".read": "auth != null",
       ".write": "auth != null"
     }
   }
   ```
6. ⚙️ **Configuración del proyecto** → Agregar app → **Android** → paquete `cl.inacap.monitorestructural` → Registrar → **descarga `google-services.json`**.

> Haz el paso 4 antes del 6. Si no, `google-services.json` sale sin la dirección de la base de datos.

## 2. Abrir el proyecto en Android Studio

1. En GitHub: **Code → Download ZIP** y descomprímelo.
2. Android Studio → **Open** → elige la carpeta **`app-android`**, no la carpeta raíz del repo.
3. Copia `google-services.json` dentro de **`app-android/app/`**.
4. Espera a que termine **Gradle sync**. La primera vez descarga varias cosas y puede tardar de 5 a 15 minutos.
   - Si aparece el asistente para actualizar el plugin de Android (*Upgrade Assistant*), elige **Remind me later**.
5. En el celular activa el **modo desarrollador** (Ajustes → Acerca del teléfono → toca 7 veces "Número de compilación") y luego la **Depuración USB**.
6. Conecta el celular por USB y aprieta **▶ Run**.

## 3. Instalar en el segundo celular

- **Opción A:** conectar el otro celular por USB y volver a apretar ▶ Run.
- **Opción B:** Build → Build App Bundle(s) / APK(s) → **Build APK(s)**. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. Se envía al otro celular y se instala aceptando "instalar apps desconocidas".

## 4. Guion de la demo

| Paso | Celular A (Activador) | Celular B (Monitoreo) | Qué se demuestra |
|---|---|---|---|
| 1 | Login con contraseña mala y después con la buena | Login | Autenticación |
| 2 | Abre **Activador**, deja la frecuencia en 40 Hz y aprieta **Registrar línea base** | Abre **Monitoreo**: aparece Edificio A en verde | Conexión entre los dos celulares |
| 3 | Activa **Envío automático** y baja el slider a 30 Hz | Edificio A pasa a **rojo: Daño, caída 25 %** | Datos en tiempo real |
| 4 | Sube a 40 Hz y activa **Simular sismo** | Pasa a **amarillo: Sismo** | Prioridad de estados |
| 5 | Elige NODO-02 y envía | Aparecen 2 estructuras, la más grave arriba | Varias estructuras |
| 6 | Apaga el envío automático | A los 15 s pasa a **gris: Sin datos** | Detección de nodo caído |
| 7 | — | Apaga el WiFi y los datos: aparece el aviso **Sin conexión**. Al encenderlos se actualiza sola | Estabilidad de la conexión |
| 8 | — | Toca una estructura: **historial** de las últimas 10 mediciones | Guardar y consultar datos |
| 9 | Muestra en la consola de Firebase los datos y las **Reglas** | Cierra sesión | Seguridad |

## 5. Problemas comunes

| Problema | Solución |
|---|---|
| `File google-services.json is missing` | Falta copiar el archivo en `app-android/app/` |
| "Correo o contraseña incorrectos" con los datos buenos | Revisa que Correo/contraseña esté habilitado y que el usuario exista en Authentication |
| La lista no muestra nada y aparece *Permission denied* | Faltan publicar las reglas o no hay sesión iniciada |
| El Activador se queda en "Enviando…" | No hay internet; se envía solo al volver la conexión |
| Todas las estructuras aparecen en gris aunque se estén enviando | La hora del celular está desfasada: activa **fecha y hora automáticas** en los dos celulares |
| El sync falla por la versión de Java o de Gradle | Sacar captura del error y revisarlo |
