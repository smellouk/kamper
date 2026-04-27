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
                implementation(project(":kamper:engine"))
                implementation(project(":kamper:modules:cpu"))
                implementation(project(":kamper:modules:fps"))
                implementation(project(":kamper:modules:memory"))
                implementation(project(":kamper:modules:network"))
                implementation(project(":kamper:modules:issues"))
                implementation(project(":kamper:modules:jank"))
                implementation(project(":kamper:modules:gc"))
                implementation(project(":kamper:modules:thermal"))
                implementation(project(":kamper:ui:kmm"))
            }
        }
    }
}
