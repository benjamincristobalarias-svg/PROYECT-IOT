# Guía: ejecutar la app

> Hecho con IA (Claude).

## 1. Abrir el proyecto

1. En GitHub: **Code → Download ZIP** y descomprimirlo.
2. Android Studio → **Open** → elegir la carpeta **`app-android`**, no la carpeta raíz del repo.
3. Esperar el **Gradle Sync**. La primera vez descarga varias cosas y puede tardar varios minutos.

## 2. Ejecutar

- **En emulador:** elegir un dispositivo virtual y apretar **▶ Run**.
- **En celular:** activar el modo desarrollador (Ajustes → Acerca del teléfono → tocar 7 veces "Número de compilación"), luego la **Depuración USB**, conectar el celular por USB y apretar **▶ Run**.

La app funciona sin internet: los datos vienen del simulador.

## 3. Qué mostrar en clase

| Momento | Qué se ve | Qué se explica |
|---|---|---|
| Al abrir | Bodega Norte "Desconectado, sin lecturas" | Sin datos se muestra como `null`, nunca como 0 |
| 6 s | Edificio B en rojo: "Daño", caída 14,3 % | La frecuencia bajó más de 5 % respecto de la línea base |
| 12 s | Bodega Norte desconectada con "Última lectura guardada; no es actual" | Conexión y condición son cosas distintas |
| 15 s | Edificio A en amarillo: "Sismo" | Amplitud alta sin caída de frecuencia |
| Girar el celular | La lista sigue funcionando | El ViewModel guarda el estado |
| Cabecera | A los 12 s: "3 estructuras · 2 conectadas · 1 con daño · 0 con sismo" | Contadores calculados desde la misma lista |

Además, en Android Studio abrir `app/src/test/.../SimulacionTest.kt` y apretar ▶ junto a la clase: las 5 pruebas deben pasar.

## 4. Problemas comunes

| Problema | Qué revisar |
|---|---|
| No reconoce `collectAsStateWithLifecycle` | Que estén las dependencias de Lifecycle en `app/build.gradle.kts` y hacer Sync |
| Pide otro compileSdk o versión de AGP | Aceptar la actualización que indique Android Studio |
| La lista vuelve al inicio al volver a la app | Es normal: el simulador se reinicia después de 5 s en segundo plano |
