import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id(Libs.Plugins.Compose.id)
    id(Libs.Plugins.Compose.kotlinPluginId) version Versions.kotlin
    id("com.android.application")
}

android {
    namespace = "com.smellouk.kamper.samples.compose"
    compileSdk = Config.compileSdk
    defaultConfig {
        applicationId = "com.smellouk.kamper.samples.compose"
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
    }
    compileOptions {
        sourceCompatibility = Config.sourceCompatibility
        targetCompatibility = Config.targetCompatibility
    }
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    wasmJs {
        outputModuleName = "compose"
        browser {
            commonWebpackConfig {
                outputFileName = "compose.js"
            }
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                implementation(project(Modules.ENGINE))
                implementation(project(Modules.Performances.CPU))
                implementation(project(Modules.Performances.FPS))
                implementation(project(Modules.Performances.MEMORY))
                implementation(project(Modules.Performances.NETWORK))
                implementation(project(Modules.Performances.ISSUES))
                implementation(project(Modules.Performances.JANK))
                implementation(project(Modules.Performances.GC))
                implementation(project(Modules.Performances.THERMAL))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation(project(Modules.Ui.ANDROID))
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(Modules.Ui.ANDROID))
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.smellouk.kamper.samples.compose.MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "Kamper"
                packageVersion = "1.0.0"
            }
        }
    }
}
