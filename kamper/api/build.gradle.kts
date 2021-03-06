plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    sourceSets {
        android {
            publishLibraryVariants("release")
        }

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