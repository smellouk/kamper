import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
            export(project(":kamper:engine"))
            export(project(":kamper:modules:cpu"))
            export(project(":kamper:modules:fps"))
            export(project(":kamper:modules:memory"))
            export(project(":kamper:modules:network"))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kamper:engine"))
                api(project(":kamper:modules:cpu"))
                api(project(":kamper:modules:fps"))
                api(project(":kamper:modules:memory"))
                api(project(":kamper:modules:network"))
            }
        }
    }
}
