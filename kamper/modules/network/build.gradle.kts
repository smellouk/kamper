plugins {
    kotlin("multiplatform")
    id("dev.mokkery")
    id("com.android.library")
}
apply(from = projectDir.resolve("../../publish.gradle.kts"))

android {
    namespace = "io.mellouk.kamper.network"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    macosX64()
    macosArm64()

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
            }
        }
    }
}
