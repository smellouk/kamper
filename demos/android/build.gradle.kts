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

    implementation(project(":libs:modules:cpu"))
    implementation(project(":libs:modules:fps"))
    implementation(project(":libs:modules:memory"))
    implementation(project(":libs:modules:network"))
    implementation(project(":libs:modules:issues"))
    implementation(project(":libs:modules:jank"))
    implementation(project(":libs:modules:gc"))
    implementation(project(":libs:modules:thermal"))
    debugImplementation(project(":libs:ui:kmm"))
    // ui module is exposing project(":libs:engine")
}
