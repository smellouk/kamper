plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("com.android.library")
    id("dev.mokkery")
    id("konitor.android.config")
    id("konitor.publish")
}

android {
    namespace = "com.smellouk.konitor.firebase"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    @Suppress("DEPRECATION") macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    // js(IR) and wasmJs are intentionally included: firebase has no KMP SDK for these
    // targets but the no-op actuals allow consumers to compile for any target without
    // platform guards. Unlike sentry-kotlin-multiplatform, konitor-firebase has no
    // KMP transitive dep that blocks JS/WasmJS.
    js(IR) {
        browser()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs {
        browser()
    }

    cocoapods {
        // Required for Kotlin/Native interop with FirebaseCrashlytics on iOS targets.
        // Consumer apps must already configure Firebase via google-services.json /
        // GoogleService-Info.plist — Konitor does not initialize Firebase (per
        // RESEARCH anti-pattern: 'Initializing Firebase SDK inside the IntegrationModule constructor').
        summary = "Konitor Firebase Crashlytics integration"
        homepage = "https://github.com/smellouk/konitor"
        ios.deploymentTarget = "13.0"
        osx.deploymentTarget = "10.15"
        framework {
            baseName = "konitor_firebase"
            isStatic = true
        }
        pod("FirebaseCrashlytics") {
            version = "~> 11.0"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.firebase.crashlytics)
            }
        }
    }
}

// Skip iOS pod build and cinterop tasks when the iOS Platform component isn't installed.
// On Xcode 26+ the iOS SDK stub appears in `xcodebuild -showsdks` but `generic/platform=iOS`
// destinations are still ineligible until the platform is installed via Xcode > Settings > Components.
// The `onlyIf` block runs at execution time (after podInstallSyntheticIos creates Pods.xcodeproj).
// When cinterop is skipped, any stale invalid KLIB output from prior runs is deleted so that
// commonizeCInterop (which aggregates all targets) doesn't choke on the corrupt artifact.
afterEvaluate {
    val iosAvailableSpec: Spec<Task> = Spec { _ ->
        val podsProj = File(project.projectDir, "build/cocoapods/synthetic/ios/Pods/Pods.xcodeproj")
        if (!podsProj.exists()) return@Spec true
        val proc = ProcessBuilder(
            "xcodebuild", "-showdestinations",
            "-project", podsProj.absolutePath,
            "-scheme", "FirebaseCrashlytics"
        ).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        !out.contains("not installed")
    }
    tasks.matching { t ->
        (t.name.startsWith("podBuildFirebaseCrashlytics") ||
         t.name.startsWith("cinteropFirebaseCrashlyticsIos")) &&
        !t.name.contains("Macos", ignoreCase = true)
    }.configureEach { onlyIf(iosAvailableSpec) }

    // commonizeCInterop aggregates all cinterop KLIBs including iOS. When iOS cinterop is
    // skipped, stale invalid KLIB directories from prior runs break the commonizer. Clean them
    // in doFirst so the commonizer only sees valid macOS KLIBs.
    tasks.named("commonizeCInterop").configure {
        doFirst {
            if (!iosAvailableSpec.isSatisfiedBy(this)) {
                listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
                    File(project.projectDir,
                        "build/classes/kotlin/$target/main/cinterop/firebase-cinterop-FirebaseCrashlytics"
                    ).deleteRecursively()
                }
            }
        }
    }
}
