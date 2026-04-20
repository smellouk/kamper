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
