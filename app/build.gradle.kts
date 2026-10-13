plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release APKs come out unsigned and are signed by hand with the Numio key,
// so the key never leaves RR's own computer.
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

    buildTypes {
        release {
            // R8: shrinks unused library code and starts faster
            optimization {
                enable = true
            }
        }
        // Test builds from GitHub: same as release (R8 on), but signed with a throwaway
        // test key and installed next to the real app as "Notes Test".
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
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
    // Newer fragment than the lock library brings; the old one crashes file pickers
    implementation(libs.androidx.fragment.ktx)

    // Rich text editor
    implementation(libs.richeditor.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
