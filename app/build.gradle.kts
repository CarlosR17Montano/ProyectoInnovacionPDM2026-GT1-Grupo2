plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "sv.edu.ues.entregatrack"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "sv.edu.ues.entregatrack"
        minSdk = 23
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // CameraX para vista previa y captura de fotografias
    implementation("androidx.camera:camera-camera2:1.5.1")
    implementation("androidx.camera:camera-lifecycle:1.5.1")
    implementation("androidx.camera:camera-view:1.5.1")

    // Glide para mostrar imagenes guardadas o descargadas
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Google Maps para mostrar la ubicacion del repartidor
    implementation("com.google.android.gms:play-services-maps:20.0.0")

    // FragmentActivity para manejar SupportMapFragment de Google Maps
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}