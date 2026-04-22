plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.smellouk.kamper.android"
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = "com.smellouk.kamper.android"
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = Config.sourceCompatibility
        targetCompatibility = Config.targetCompatibility
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Config.jvmTarget))
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(Libs.Android.Androidx.appCompat)
    implementation(Libs.Android.Androidx.material)
    implementation(Libs.Android.Androidx.constraintlayout)
    implementation(Libs.Android.Androidx.core_ktx)

    implementation(project(Modules.ENGINE))
    implementation(project(Modules.Performances.CPU))
    implementation(project(Modules.Performances.FPS))
    implementation(project(Modules.Performances.MEMORY))
    implementation(project(Modules.Performances.NETWORK))
    implementation(project(Modules.Performances.ISSUES))
    implementation(project(Modules.Performances.JANK))
    implementation(project(Modules.Performances.GC))
    implementation(project(Modules.Performances.THERMAL))
    debugImplementation(project(Modules.Ui.ANDROID))
}
