plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    macosX64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.macos.main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.macos.main"
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
