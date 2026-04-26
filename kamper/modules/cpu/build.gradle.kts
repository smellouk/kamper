plugins {
    kotlin("multiplatform")
    id("dev.mokkery")
    id("com.android.library")
}
apply(from = projectDir.resolve("../../publish.gradle.kts"))

android {
    namespace = "io.mellouk.kamper.cpu"
    buildFeatures { buildConfig = true }
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }
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
    tvosArm64()
    tvosSimulatorArm64()
    js(IR) {
        browser()
    }
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(Modules.API))
                implementation(Libs.Kmm.Coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Libs.Android.Coroutines.android)
                implementation(Libs.Android.Androidx.annotationx)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(Libs.Android.Tests.runner)
                implementation(Libs.Android.Tests.mockk_android)
            }
        }
    }
}
