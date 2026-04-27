plugins {
    id("kamper.kmp.library")
}

android {
    namespace = "com.smellouk.kamper.api"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
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
