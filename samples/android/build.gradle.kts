plugins {
    id("com.android.application")
    kotlin("android")
}

android {
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = Config.jvmTarget
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