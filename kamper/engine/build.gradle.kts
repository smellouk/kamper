plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    android()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(Modules.API))
                implementation(Libs.Kmm.stdlib)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Libs.Android.Androidx.annotationx)
                implementation(Libs.Android.Androidx.lifecycleCommon)
            }
        }
    }
}