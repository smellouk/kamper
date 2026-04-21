plugins {
    kotlin("multiplatform")
    id("dev.mokkery")
    id("com.android.library")
}
apply(from = projectDir.resolve("../../publish.gradle.kts"))

android {
    namespace = "io.mellouk.kamper.jank"
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
    }
}
