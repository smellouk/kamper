plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.smellouk.kamper.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smellouk.kamper.android"
        minSdk = 21
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)

    implementation(project(":kamper:engine"))
    implementation(project(":kamper:modules:cpu"))
    implementation(project(":kamper:modules:fps"))
    implementation(project(":kamper:modules:memory"))
    implementation(project(":kamper:modules:network"))
    implementation(project(":kamper:modules:issues"))
    implementation(project(":kamper:modules:jank"))
    implementation(project(":kamper:modules:gc"))
    implementation(project(":kamper:modules:thermal"))
    debugImplementation(project(":kamper:ui:kmm"))
}
