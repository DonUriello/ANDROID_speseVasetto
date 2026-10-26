plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Numero progressivo della build: su GitHub Actions è il numero di esecuzione del workflow,
// così ogni APK ha un versionCode più alto del precedente e si installa come aggiornamento.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "it.elia.gestionespese"
    compileSdk = 37

    defaultConfig {
        applicationId = "it.elia.gestionespese"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    // Firma di release: sempre la stessa chiave, letta dai secret del repository (vedi docs/FIRMA.md).
    // Senza le variabili d'ambiente l'APK di release resta non firmato e non va installato.
    val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

room {
    // Schema esportato e versionato: serve per scrivere e verificare le migrazioni.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
