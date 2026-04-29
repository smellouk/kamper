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
            export(project(":libs:engine"))
            export(project(":libs:modules:cpu"))
            export(project(":libs:modules:fps"))
            export(project(":libs:modules:memory"))
            export(project(":libs:modules:network"))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:engine"))
                api(project(":libs:modules:cpu"))
                api(project(":libs:modules:fps"))
                api(project(":libs:modules:memory"))
                api(project(":libs:modules:network"))
            }
        }
    }
}
