plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.fps"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kamper:api"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
    }
}
