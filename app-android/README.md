# App Android: Monitor Estructural

> Hecho con IA (Claude), a partir de la plantilla y el Ejemplo02 del profe.

App hecha siguiendo la guía del profe (**GUIAAPPKOTLIN**) y su **Ejemplo02**: Android Studio + Kotlin + Jetpack Compose + ViewModel, con **Firebase**.

| Pantalla | Qué hace |
|---|---|
| **Login** | Correo y contraseña con Firebase Authentication (Ingresar / Crear cuenta) |
| **Activador** | Hace de sensor: estructura (RadioButton), frecuencia y amplitud (Slider), sensor conectado (Switch) y botón **Enviar a Firebase**. Tiene envío automático (simulador cada 3 s) y muestra cuántos envíos confirmó Firebase y en cuántos ms |
| **Monitoreo** | Muestra en vivo lo que hay en Firebase: una tarjeta por estructura con frecuencia, línea base, caída y estado |

Con dos celulares: uno en **Activador** y el otro en **Monitoreo**. Los datos pasan por Firebase (WiFi o datos móviles).

## Cómo abrirla

1. Firebase ya está configurado (proyecto `monitor-estructural`). Descargar `google-services.json` desde la consola (pasos en [`docs/app/guia_puesta_en_marcha.md`](../docs/app/guia_puesta_en_marcha.md)).
2. Copiarlo dentro de **`app-android/app/`**. No se sube al repo.
3. Android Studio → **Open** → carpeta **`app-android`** → esperar el **Gradle Sync**.
4. **▶ Run** en un emulador o celular con internet.
5. La primera vez: escribir un correo y una contraseña (mínimo 8 caracteres) y apretar **Crear cuenta**.

## Organización (igual que la guía)

```
app/src/main/java/cl/inacap/monitorestructural/
    MainActivity.kt                        crea repositorios y ViewModels; muestra Login o la app según la sesión
    model/Estructura.kt                    estructura y lectura; daño, sismo, estructuras instaladas
    model/Credenciales.kt                  revisa correo y contraseña antes de enviarlos
    data/AuthRepository.kt                 login, crear cuenta y salir (Firebase Authentication)
    data/EstructurasRepository.kt          contrato de la fuente de datos
    data/FakeEstructurasRepository.kt      simulador del envío automático (Flow cada 3 s)
    data/FirebaseEstructurasRepository.kt  enviar(), escuchar la base (callbackFlow) y la conexión
    ui/LoginScreen.kt + LoginViewModel.kt
    ui/ActivadorScreen.kt + ActivadorViewModel.kt
    ui/MonitoreoScreen.kt + EstructurasViewModel.kt
    ui/PrincipalScreen.kt                  barra superior y pestañas Monitoreo / Activador
```

## Datos en Firebase

```
estructuras/
  NODO-01/  nombre, ubicacion, frecuenciaBaseHz, conectada, lectura { frecuenciaHz, amplitud, measuredAtMillis }
historial/
  NODO-01/  {clave}: { frecuenciaHz, amplitud, measuredAtMillis, usuario }   <- uno por envío
```

Reglas ([`firebase/database.rules.json`](firebase/database.rules.json)): solo con sesión (`auth != null`), validan tipos y rangos (0–100 Hz), rechazan campos desconocidos, el historial no se puede modificar ni borrar y `usuario` tiene que ser el mismo que inició sesión.

## Estados

| Estado | Cuándo | Color |
|---|---|---|
| Daño | La frecuencia bajó más de 5 % respecto de la línea base | Rojo |
| Sismo | Sin daño, pero la amplitud supera 230.000 | Amarillo |
| Normal | Frecuencia cerca de la línea base | Verde |
| Sin evaluación actual | Sensor desconectado; su última lectura se muestra como "no actual" | — |
| Sin datos | Todavía no llega ninguna lectura (`null`, nunca un 0 inventado) | — |

## Pruebas

`SimulacionTest.kt` y `LoginYActivadorTest.kt` (11 pruebas): reglas de daño y sismo, simulador, revisión de correo y contraseña, y la lectura que arma el Activador. Se ejecutan en Android Studio con el botón ▶ junto a cada clase.
