plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:api"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.lifecycle.common)
            }
        }
    }
}
