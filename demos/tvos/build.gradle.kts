plugins {
    alias(libs.plugins.kotlin.multiplatform)
}


kotlin {
    tvosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.tvos.main"
                // Kotlin/Native 2.3.x tvOS UIKit klib is missing UIMotionEffect descriptor.
                freeCompilerArgs += listOf("-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=WARNING")
            }
        }
    }
    tvosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.tvos.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=WARNING")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":libs:engine"))
                implementation(project(":libs:modules:cpu"))
                implementation(project(":libs:modules:fps"))
                implementation(project(":libs:modules:memory"))
                implementation(project(":libs:modules:network"))
                implementation(project(":libs:modules:issues"))
                implementation(project(":libs:modules:jank"))
                implementation(project(":libs:modules:gc"))
                implementation(project(":libs:modules:gpu"))
                implementation(project(":libs:modules:thermal"))
            }
        }
    }
}
