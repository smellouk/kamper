plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.smellouk.kamper.samples"
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = "com.smellouk.kamper.samples"
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
}
