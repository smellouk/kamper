plugins {
    id("konitor.kmp.library")
    id("konitor.publish")
}

android {
    namespace = "com.smellouk.konitor.cpu"
    buildFeatures { buildConfig = true }
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

    // Mach host_statistics for direct CPU tick counters — no subprocess overhead.
    listOf(macosArm64(), @Suppress("DEPRECATION") macosX64()).forEach { target ->
        target.compilations.getByName("main") {
            cinterops.create("cpuInfo") {
                defFile(project.file("src/nativeInterop/cinterop/cpuInfo.def"))
                packageName("com.smellouk.konitor.cpu.cinterop")
            }
        }
    }
}
