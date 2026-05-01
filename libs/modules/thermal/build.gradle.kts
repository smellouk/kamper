plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.thermal"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:api"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.annotation)
            }
        }
    }

    // NSProcessInfo.thermalState is absent from KN macOS pre-built bindings; expose it via cinterop.
    listOf(macosArm64(), @Suppress("DEPRECATION") macosX64()).forEach { target ->
        target.compilations.getByName("main") {
            cinterops.create("thermalState") {
                defFile(project.file("src/nativeInterop/cinterop/thermalState.def"))
                packageName("com.smellouk.kamper.thermal.cinterop")
            }
        }
    }
}
