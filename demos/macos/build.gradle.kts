plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    @Suppress("DEPRECATION") val macosX64 = macosX64()
    val macosArm64 = macosArm64()
    listOf(macosX64, macosArm64).forEach {
        it.binaries {
            executable {
                entryPoint = "com.smellouk.konitor.macos.main"
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
