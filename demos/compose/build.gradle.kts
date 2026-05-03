import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.smellouk.konitor.compose"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.smellouk.konitor.compose"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    applyDefaultHierarchyTemplate()
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
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                implementation(project(":libs:engine"))
                implementation(project(":libs:modules:cpu"))
                implementation(project(":libs:modules:fps"))
                implementation(project(":libs:modules:memory"))
                implementation(project(":libs:modules:network"))
                implementation(project(":libs:modules:issues"))
                implementation(project(":libs:modules:jank"))
                implementation(project(":libs:modules:gc"))
                implementation(project(":libs:modules:gpu"))
                implementation(project(":libs:modules:thermal"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(project(":libs:ui:kmm"))
            }
        }

        named("iosMain") {
            dependencies {
                implementation(project(":libs:ui:kmm"))
            }
        }

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
            mainClass = "com.smellouk.konitor.compose.MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "K|Compose"
                packageVersion = "1.0.0"
            }
        }
    }
}
