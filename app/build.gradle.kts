plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.delassierras.radio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.delassierras.radio"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.8.0"
    }
    // Signing credentials are injected only by GitHub Actions; never committed.
    signingConfigs {
        create("publicRelease") {
            val path = System.getenv("RADIO_KEYSTORE_PATH")
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = System.getenv("RADIO_KEYSTORE_PASSWORD")
                keyAlias = "radio-release"
                keyPassword = System.getenv("RADIO_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("publicRelease")
            isMinifyEnabled = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
