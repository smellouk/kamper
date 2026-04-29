plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    iosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.ios.main"
            }
        }
    }
    iosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.ios.main"
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
                implementation(project(":libs:modules:thermal"))
                implementation(project(":libs:ui:kmm"))
            }
        }
    }
}
