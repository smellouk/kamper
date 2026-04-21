import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
}

kotlin {
    val xcf = XCFramework("Kamper")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Kamper"
            isStatic = true
            xcf.add(this)
            export(project(Modules.ENGINE))
            export(project(Modules.Performances.CPU))
            export(project(Modules.Performances.FPS))
            export(project(Modules.Performances.MEMORY))
            export(project(Modules.Performances.NETWORK))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(Modules.ENGINE))
                api(project(Modules.Performances.CPU))
                api(project(Modules.Performances.FPS))
                api(project(Modules.Performances.MEMORY))
                api(project(Modules.Performances.NETWORK))
            }
        }
    }
}
