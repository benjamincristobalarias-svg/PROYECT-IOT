// Hecho con IA (Claude)
// Archivo de nivel superior: declara las versiones de los plugins.
// AGP 9 compila Kotlin por sí solo (Kotlin integrado), por eso no se
// declara el plugin org.jetbrains.kotlin.android.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
