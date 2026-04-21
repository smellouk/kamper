plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id(Libs.Plugins.Compose.id)
    id(Libs.Plugins.Compose.kotlinPluginId) version Versions.kotlin
}

android {
    namespace = "com.smellouk.kamper.ui"
    compileSdk = Config.compileSdk
    defaultConfig { minSdk = Config.minSdk }
    compileOptions {
        sourceCompatibility = Config.sourceCompatibility
        targetCompatibility = Config.targetCompatibility
    }
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(Modules.ENGINE))
            implementation(project(Modules.Performances.CPU))
            implementation(project(Modules.Performances.FPS))
            implementation(project(Modules.Performances.MEMORY))
            implementation(project(Modules.Performances.NETWORK))
            implementation(Libs.Kmm.Coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(Libs.Android.Coroutines.android)
            implementation(Libs.Android.Androidx.core_ktx)
            implementation(Libs.Android.Androidx.appCompat)
        }
    }
}
