plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("dev.mokkery")
    id("kamper.android.config")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.sentry"
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
    // The other kamper modules support JS/WasmJS because they only depend on kamper:api
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
