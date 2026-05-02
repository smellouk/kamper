plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.gpu"
    externalNativeBuild {
        cmake {
            path("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
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

        val jvmMain by getting {
            dependencies {
                implementation(libs.oshi.core)
            }
        }
    }

    // IOKit IOAccelerator binding — macOS only (arm64 + x64).
    // iOS/tvOS deliberately excluded: IOAccelerator is a private framework on iOS/tvOS;
    // attaching the cinterop there risks App Store rejection (per D-07 + 23-RESEARCH Pitfall 3).
    listOf(macosArm64(), @Suppress("DEPRECATION") macosX64()).forEach { target ->
        target.compilations.getByName("main") {
            cinterops.create("gpuInfo") {
                defFile(project.file("src/nativeInterop/cinterop/gpuInfo.def"))
                packageName("com.smellouk.kamper.gpu.cinterop")
            }
        }
    }
}
