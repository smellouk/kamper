plugins {
    kotlin("multiplatform")
    id("dev.mokkery")
    id("com.android.library")
}

android {
    namespace = "io.mellouk.kamper.api"
}

kotlin {
    androidTarget()
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
                implementation(Libs.Kmm.Coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(Libs.Kmm.Tests.coroutines)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Libs.Android.Coroutines.android)
            }
        }
    }
}
