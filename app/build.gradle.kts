import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing is read from keystore.properties (local, never committed)
// or from environment variables (GitHub Actions). If neither is present,
// the release build falls back to the debug key so it still installs.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.rr.numio.notes"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rr.numio.notes"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        val storePath = signingValue("storeFile", "NUMIO_KEYSTORE_PATH")
        if (storePath != null) {
            create("release") {
                storeFile = file(storePath)
                storePassword = signingValue("storePassword", "NUMIO_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "NUMIO_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "NUMIO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Notes storage
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Settings (theme, accent)
    implementation(libs.androidx.datastore.preferences)

    // Screens
    implementation(libs.androidx.navigation.compose)

    // App lock (fingerprint / screen lock)
    implementation(libs.androidx.biometric)

    // Rich text editor
    implementation(libs.richeditor.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
