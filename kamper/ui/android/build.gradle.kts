plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("dev.mokkery")
    id(Libs.Plugins.Compose.id)
    id(Libs.Plugins.Compose.kotlinPluginId) version Versions.kotlin
}

android {
    namespace = "com.smellouk.kamper.ui"
    compileSdk = Config.compileSdk
    defaultConfig {
        minSdk = Config.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = Config.sourceCompatibility
        targetCompatibility = Config.targetCompatibility
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
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
            implementation(project(Modules.Performances.ISSUES))
            implementation(project(Modules.Performances.JANK))
            implementation(project(Modules.Performances.GC))
            implementation(project(Modules.Performances.THERMAL))
            implementation(Libs.Kmm.Coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(Libs.Android.Coroutines.android)
            implementation(Libs.Android.Androidx.core_ktx)
            implementation(Libs.Android.Androidx.appCompat)
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(Libs.Kmm.Tests.coroutines)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(Libs.Android.Tests.runner)
                implementation(Libs.Android.Tests.junit_ext)
                implementation(Libs.Android.Tests.mockk_android)
            }
        }
    }
}
