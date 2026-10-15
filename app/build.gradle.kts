import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// Release signing.
// Locally: keystore/keystore.properties (git-ignored).
// CI (GitHub Actions): STRADE_KEYSTORE_FILE / STRADE_KEYSTORE_PASSWORD /
//                      STRADE_KEY_ALIAS / STRADE_KEY_PASSWORD environment variables.
// When no keystore is available the release build falls back to the debug key so
// that the APK is still installable (updates then require the same key).
// ---------------------------------------------------------------------------
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore/keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingProperty(key: String, envName: String): String? =
    keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }

val releaseStoreFile: File? = signingProperty("storeFile", "STRADE_KEYSTORE_FILE")
    ?.let { path -> File(path).let { if (it.isAbsolute) it else rootProject.file(path) } }
    ?.takeIf { it.exists() }

val appVersionCode: Int = (findProperty("strade.versionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName: String = (findProperty("strade.versionName") as String?) ?: "1.0.0"

android {
    namespace = "it.stradesumisura.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.stradesumisura.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingProperty("storePassword", "STRADE_KEYSTORE_PASSWORD")
                keyAlias = signingProperty("keyAlias", "STRADE_KEY_ALIAS")
                keyPassword = signingProperty("keyPassword", "STRADE_KEY_PASSWORD")
                storeType = "PKCS12"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Strumenti, database, interfaccia e report PDF sono nel modulo condiviso con la versione Windows.
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
}
