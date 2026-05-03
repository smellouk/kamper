plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("dev.mokkery")
    id("konitor.android.config")
    id("konitor.publish")
}

android {
    namespace = "com.smellouk.konitor.sentry"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    @Suppress("DEPRECATION") macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    // js(IR) and wasmJs are intentionally excluded: sentry-kotlin-multiplatform:0.13.0
    // does not publish JS/WasmJS artifacts. Including these targets causes a KMP
    // dependency resolution failure (webMain source set cannot resolve the dep).
    // The other konitor modules support JS/WasmJS because they only depend on konitor:api
    // which also lacks JS/WasmJS-specific sentry calls.

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:api"))
                implementation(libs.sentry.kotlin.multiplatform)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
