# App Android: Monitor Estructural

> Hecho con IA (Claude), a partir de la plantilla y el Ejemplo02 del profe.

App hecha siguiendo la guía del profe (**GUIAAPPKOTLIN**) y su **Ejemplo02**: Android Studio + Kotlin + Jetpack Compose + ViewModel.

La app genera lecturas de **3 estructuras simuladas** cada 3 segundos (como el simulador ambiental de la guía, pero con frecuencia de vibración, línea base y amplitud), las **envía a Firebase Realtime Database** y **muestra lo que hay guardado en Firebase**. Más adelante el simulador se reemplaza por el sensor real.

## Cómo abrirla

1. Crear el proyecto en Firebase y descargar `google-services.json` (pasos en [`docs/app/guia_puesta_en_marcha.md`](../docs/app/guia_puesta_en_marcha.md)).
2. Copiar `google-services.json` dentro de **`app-android/app/`**. No se sube al repo.
3. Android Studio → **Open** → elegir la carpeta **`app-android`** → esperar el **Gradle Sync**.
4. Elegir un emulador o un celular con internet y apretar **▶ Run**.

## Organización (igual que la guía)

```
app/src/main/java/cl/inacap/monitorestructural/
    MainActivity.kt                    crea el repositorio y el ViewModel, y muestra la pantalla
    model/Estructura.kt                datos de una estructura y de una lectura (daño y sismo)
    data/EstructurasRepository.kt      contrato de la fuente de datos
    data/FakeEstructurasRepository.kt  simulador: una lista nueva cada 3 s (Flow)
    data/FirebaseEstructurasRepository.kt  guarda en Firebase y lee desde Firebase (callbackFlow)
    ui/EstructurasViewModel.kt         estado de la pantalla (StateFlow)
    ui/EstructurasScreen.kt            pantalla en Compose (LazyColumn + Card)
```

```
FakeEstructurasRepository --(cada 3 s)--> EstructurasViewModel --guardar()--> Firebase
                                                                                 |
EstructurasScreen <-- StateFlow<EstructurasUiState> <-- observeEstructuras() <---+
```

## Datos en Firebase

```
estructuras/
  NODO-01/  nombre, ubicacion, frecuenciaBaseHz, conectada, lectura { frecuenciaHz, amplitud, measuredAtMillis }
  NODO-02/  ...
  NODO-03/  ...
historial/
  NODO-01/  {clave}: { frecuenciaHz, amplitud, measuredAtMillis }   <- una por cada envío
```

Reglas de la base: [`firebase/database.rules.json`](firebase/database.rules.json).

## Estados

| Estado | Cuándo | Color |
|---|---|---|
| Daño | La frecuencia bajó más de 5 % respecto de la línea base | Rojo |
| Sismo | Sin daño, pero la amplitud supera 230.000 | Amarillo |
| Normal | Frecuencia cerca de la línea base | Verde |
| Desconectado | El sensor no está conectado; se muestra su última lectura como "no actual" | — |
| Sin datos | Todavía no llega ninguna lectura (`null`, nunca un 0 inventado) | — |

## Qué pasa en la simulación

| Tiempo | Edificio A | Edificio B | Bodega Norte |
|---|---|---|---|
| 0 s | Normal | Normal | Desconectada, sin datos |
| 3 s | Normal | Normal | Conectada |
| 6 s | Normal | **Daño** (caída 14,3 %) | Conectada |
| 9 s | Normal | **Daño** | Conectada |
| 12 s | Normal | **Daño** | Desconectada, conserva la lectura de los 9 s |
| 15 s | **Sismo** | Normal | Conectada |

Después se repite la secuencia.

## Pruebas

`app/src/test/.../SimulacionTest.kt` revisa el simulador y las reglas de daño y sismo. Se ejecuta en Android Studio con el botón ▶ junto a la clase.
