plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
apply(from = projectDir.resolve("../../publish.gradle.kts")).also {
    extra["moduleName"] = "cpu-module"
}

kotlin {
    android()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(Modules.API))
                implementation(Libs.Kmm.Coroutines.core)
                implementation(Libs.Kmm.stdlib)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Libs.Android.Coroutines.android)
            }
        }
    }
}