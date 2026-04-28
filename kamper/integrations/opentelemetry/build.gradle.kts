plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("dev.mokkery")
    id("kamper.android.config")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.opentelemetry"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
    }
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kamper:api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        // OTel Java SDK provides full Metrics API; bind on JVM and Android only.
        // Per RESEARCH Pitfall 1: opentelemetry-kotlin 0.3.0 has NO Metrics API.
        // Per RESEARCH Pitfall 2: opentelemetry-kotlin does not target macOS or wasmJs.
        val androidMain by getting {
            dependencies {
                implementation(libs.opentelemetry.sdk.metrics)
                implementation(libs.opentelemetry.exporter.otlp)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.opentelemetry.sdk.metrics)
                implementation(libs.opentelemetry.exporter.otlp)
            }
        }
    }
}
