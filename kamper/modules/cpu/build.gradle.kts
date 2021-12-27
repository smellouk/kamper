plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    compileSdk = Config.compileSdk
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
    }
    compileOptions {
        sourceCompatibility = Config.sourceCompatibility
        targetCompatibility = Config.targetCompatibility
    }
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
        val commonTest by getting {
            dependencies {
                implementation(Libs.Kmm.Tests.mockk)
                implementation(kotlin(Libs.Kmm.Tests.kcommon))
                implementation(kotlin(Libs.Kmm.Tests.kannotationscommon))
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