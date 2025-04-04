plugins {
    id("com.android.application")
}

android {
    namespace = "com.phototext" // Namespace corretto
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phototext" // Coerente con il namespace
        minSdk = 26
        targetSdk = 35
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation ("androidx.core:core:1.6.0")
    implementation ("com.google.android.gms:play-services-base:18.1.0")
    implementation ("androidx.core:core-ktx:1.13.0") // Per le coroutine
    implementation(libs.google.material.v1100)
    implementation (libs.androidx.media3.exoplayer)
    // ML Kit per il riconoscimento del testo
    implementation(libs.mlkit.text.recognition.v1600)
    // Per la gestione dei permessi (Java)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    // Per la gestione dei file multimediali
    implementation(libs.androidx.media)
    // Per il picker di file moderno
    implementation(libs.androidx.documentfile)
    implementation(libs.feature.delivery)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
}
