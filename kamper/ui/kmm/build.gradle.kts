plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    alias(libs.plugins.kotlin.compose)
    id("kamper.android.config")
}

android {
    namespace = "com.smellouk.kamper.ui"
    // compileSdk, minSdk, compileOptions provided by kamper.android.config

    testOptions {
        unitTests {
            // Prevents RuntimeException("Method not mocked") when JVM unit tests call
            // Android framework methods (e.g., SystemClock.elapsedRealtimeNanos()).
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":kamper:engine"))
            implementation(project(":kamper:modules:cpu"))
            implementation(project(":kamper:modules:fps"))
            implementation(project(":kamper:modules:memory"))
            implementation(project(":kamper:modules:network"))
            implementation(project(":kamper:modules:issues"))
            implementation(project(":kamper:modules:jank"))
            implementation(project(":kamper:modules:gc"))
            implementation(project(":kamper:modules:thermal"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
        }
        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidUnitTest").dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}
