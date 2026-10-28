// Hecho con IA (Claude)
plugins {
    id("com.android.application")
    // Lee app/google-services.json y conecta la app con el proyecto de Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "cl.inacap.monitorestructural"
    compileSdk = 36

    defaultConfig {
        applicationId = "cl.inacap.monitorestructural"
        minSdk = 26          // Android 8.0 o superior
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Librerías base de Android y Material Design
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.13.0")

    // Firebase: la "bom" elige versiones compatibles entre sí
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
    implementation("com.google.firebase:firebase-auth")      // inicio de sesión
    implementation("com.google.firebase:firebase-database")  // base de datos en tiempo real
}
