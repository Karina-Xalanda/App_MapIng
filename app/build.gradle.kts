plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.maping"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.maping"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)      // Para Login
    implementation(libs.firebase.firestore) // Para Base de Datos
    implementation(libs.firebase.storage)   // Para Imágenes

    implementation(libs.play.services.auth)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.play.services.auth)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.location) // GPS

    // --- AGREGA ESTAS LÍNEAS PARA EL MAPA ---
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    implementation("io.coil-kt:coil-compose:2.6.0") // Añadir esta línea
    implementation("com.google.code.gson:gson:2.10.1")

}