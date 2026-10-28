# App Android: Monitor Estructural

> Hecho con IA (Claude), a partir de la plantilla y el Ejemplo02 del profe.

App hecha siguiendo la guía del profe (**GUIAAPPKOTLIN**) y su **Ejemplo02**: Android Studio + Kotlin + Jetpack Compose + ViewModel.

Por ahora muestra **3 estructuras simuladas** que se actualizan cada 3 segundos, igual que el simulador ambiental de la guía, pero con los datos de nuestro proyecto: frecuencia de vibración, línea base y amplitud. La conexión con Firebase y con el sensor real viene en la guía siguiente.

## Cómo abrirla

1. Android Studio → **Open** → elegir la carpeta **`app-android`**.
2. Esperar el **Gradle Sync**.
3. Elegir un emulador o un celular y apretar **▶ Run**.

## Organización (igual que la guía)

```
app/src/main/java/cl/inacap/monitorestructural/
    MainActivity.kt                    crea el repositorio y el ViewModel, y muestra la pantalla
    model/Estructura.kt                datos de una estructura y de una lectura (daño y sismo)
    data/EstructurasRepository.kt      contrato de la fuente de datos
    data/FakeEstructurasRepository.kt  simulador: una lista nueva cada 3 s (Flow)
    ui/EstructurasViewModel.kt         estado de la pantalla (StateFlow)
    ui/EstructurasScreen.kt            pantalla en Compose (LazyColumn + Card)
```

```
FakeEstructurasRepository -> Flow<List<Estructura>>
            |
   EstructurasViewModel
            |
 StateFlow<EstructurasUiState>
            |
     EstructurasScreen
```

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
