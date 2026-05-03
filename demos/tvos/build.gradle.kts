plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

tasks.register("assembleTvosSimulatorApp") {
    dependsOn("linkDebugExecutableTvosSimulatorArm64")
    val kexe = layout.buildDirectory.file("bin/tvosSimulatorArm64/debugExecutable/tvos.kexe")
    val bundleDir = layout.buildDirectory.dir("KonitorTVOS.app")
    inputs.file(kexe)
    outputs.dir(bundleDir)
    doLast {
        val bundle = bundleDir.get().asFile
        bundle.mkdirs()
        val exe = bundle.resolve("KonitorTVOS")
        kexe.get().asFile.copyTo(exe, overwrite = true)
        exe.setExecutable(true)
        bundle.resolve("Info.plist").writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>KonitorTVOS</string>
                <key>CFBundleIdentifier</key>
                <string>com.smellouk.konitor.tvos</string>
                <key>CFBundleName</key>
                <string>K|tvOS</string>
                <key>CFBundleDisplayName</key>
                <string>K|tvOS</string>
                <key>CFBundleVersion</key>
                <string>1</string>
                <key>CFBundleShortVersionString</key>
                <string>1.0</string>
                <key>CFBundlePackageType</key>
                <string>APPL</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>AppleTVSimulator</string>
                </array>
                <key>MinimumOSVersion</key>
                <string>17.0</string>
                <key>UIRequiredDeviceCapabilities</key>
                <array>
                    <string>arm64</string>
                </array>
            </dict>
            </plist>
        """.trimIndent())
    }
}

kotlin {
    tvosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.konitor.tvos.main"
                // Kotlin/Native 2.3.x tvOS UIKit klib is missing UIMotionEffect descriptor.
                freeCompilerArgs += listOf("-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=WARNING")
            }
        }
    }
    tvosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.konitor.tvos.main"
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
