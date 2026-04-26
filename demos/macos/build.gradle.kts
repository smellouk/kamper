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
                implementation(project(":kamper:engine"))
                implementation(project(":kamper:modules:cpu"))
                implementation(project(":kamper:modules:fps"))
                implementation(project(":kamper:modules:memory"))
                implementation(project(":kamper:modules:network"))
                implementation(project(":kamper:modules:issues"))
                implementation(project(":kamper:modules:jank"))
                implementation(project(":kamper:modules:gc"))
                implementation(project(":kamper:modules:thermal"))
            }
        }
    }
}
