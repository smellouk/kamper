plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

val appBundleDir = layout.buildDirectory.dir("KonitorIOS.app")

tasks.register("assembleIosSimulatorApp") {
    dependsOn("linkDebugExecutableIosSimulatorArm64")
    val kexe = layout.buildDirectory.file("bin/iosSimulatorArm64/debugExecutable/ios.kexe")
    inputs.file(kexe)
    outputs.dir(appBundleDir)
    doLast {
        val bundle = appBundleDir.get().asFile
        bundle.mkdirs()
        val exe = bundle.resolve("KonitorIOS")
        kexe.get().asFile.copyTo(exe, overwrite = true)
        exe.setExecutable(true)
        bundle.resolve("Info.plist").writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>KonitorIOS</string>
                <key>CFBundleIdentifier</key>
                <string>com.smellouk.konitor.ios</string>
                <key>CFBundleName</key>
                <string>K|iOS</string>
                <key>CFBundleDisplayName</key>
                <string>K|iOS</string>
                <key>CFBundleVersion</key>
                <string>1</string>
                <key>CFBundleShortVersionString</key>
                <string>1.0</string>
                <key>CFBundlePackageType</key>
                <string>APPL</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>iPhoneSimulator</string>
                </array>
                <key>MinimumOSVersion</key>
                <string>16.0</string>
                <key>UIRequiredDeviceCapabilities</key>
                <array>
                    <string>arm64</string>
                </array>
                <key>UISupportedInterfaceOrientations</key>
                <array>
                    <string>UIInterfaceOrientationPortrait</string>
                </array>
                <key>UILaunchScreen</key>
                <dict/>
            </dict>
            </plist>
        """.trimIndent())
    }
}

kotlin {
    iosArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.konitor.ios.main"
            }
        }
    }
    iosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.smellouk.konitor.ios.main"
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
                implementation(project(":libs:ui:kmm"))
            }
        }
    }
}
