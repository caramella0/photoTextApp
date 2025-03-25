plugins {
    id("com.android.application")
}

android {
    namespace = "com.phototext" // Namespace corretto
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phototext" // Coerente con il namespace
        minSdk = 31
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
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.google.material.v1100)
    implementation (libs.androidx.media3.exoplayer)

    // ML Kit per il riconoscimento del testo
    implementation(libs.mlkit.text.recognition.v1600)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
}
