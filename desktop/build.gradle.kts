import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ---------------------------------------------------------------------------
// STRADE su Misura per Windows 11.
// `createDistributable` produce una cartella autonoma (programma + Java incluso) che non
// richiede installazione: la CI la comprime nello ZIP portatile. I dati vengono salvati nella
// cartella «Dati» accanto al programma (vedi DataLocation.kt).
// ---------------------------------------------------------------------------
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val appVersionName: String = (findProperty("strade.versionName") as String?) ?: "1.0.0"

// jpackage accetta solo versioni numeriche (MAJOR.MINOR.PATCH).
val packageVersionName: String = Regex("""^\d+\.\d+\.\d+""").find(appVersionName)?.value ?: "1.0.0"

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
    val version = appVersionName
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("it/stradesumisura/desktop/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package it.stradesumisura.desktop
            |
            |internal object BuildInfo {
            |    const val VERSION_NAME: String = "$version"
            |}
            |""".trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildInfo)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jna)
}

compose.desktop {
    application {
        mainClass = "it.stradesumisura.desktop.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8", "-Xms64m", "-Xmx1024m")

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "STRADE su Misura"
            packageVersion = packageVersionName
            description = "Strumenti e scale di valutazione per terapisti occupazionali"
            vendor = "STRADE su Misura"
            copyright = "© 2026 STRADE su Misura"

            // Moduli Java inclusi nel runtime ridotto distribuito con il programma.
            // jdk.localedata serve per date e mesi in italiano (senza, Java usa l'inglese).
            modules(
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.prefs",
                "java.sql",
                "java.xml",
                "jdk.unsupported",
                "jdk.accessibility",
                "jdk.crypto.ec",
                "jdk.localedata",
                "jdk.zipfs",
            )

            windows {
                iconFile.set(project.file("src/main/packaging/strade.ico"))
                menuGroup = "STRADE su Misura"
                upgradeUuid = "4c1f6c1e-6a2b-4f0e-9d0b-6d3f2a6b9e51"
            }
        }
    }
}
