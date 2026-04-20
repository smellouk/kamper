plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.samples.macos.main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.kamper.samples.macos.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.ENGINE))
                implementation(project(Modules.Performances.CPU))
                implementation(project(Modules.Performances.FPS))
                implementation(project(Modules.Performances.MEMORY))
                implementation(project(Modules.Performances.NETWORK))
            }
        }
    }
}
