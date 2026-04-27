plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    alias(libs.plugins.kotlin.compose)
    id("kamper.android.config")
}

// Compose Multiplatform 1.9.x does not publish tvOS klib variants for foundation, animation,
// material3, or ui. These libraries use uikit targets (ios_arm64 / ios_simulator_arm64 native
// target attributes); compose.runtime DOES have tvos_arm64 variants.
//
// The compatibility rule below lets tvOS configurations accept ios_arm64/ios_simulator_arm64
// Compose klibs. The disambiguation rule ensures that when both tvos_arm64 AND ios_arm64
// variants are available (e.g. for project-local :kamper:engine deps), the tvos_arm64
// variant is chosen (exact match preferred). The ios_arm64 fallback only activates when
// no tvos_arm64 variant exists, which is the case for external Compose UI libraries.
val konanTargetAttr = Attribute.of("org.jetbrains.kotlin.native.target", String::class.java)
dependencies.attributesSchema {
    attribute(konanTargetAttr) {
        compatibilityRules.add(TvosToIosCompatibilityRule::class.java)
        disambiguationRules.add(TvosIosDisambiguationRule::class.java)
    }
}

abstract class TvosToIosCompatibilityRule : AttributeCompatibilityRule<String> {
    override fun execute(details: CompatibilityCheckDetails<String>) {
        val consumer = details.consumerValue
        val producer = details.producerValue
        if ((consumer == "tvos_arm64" && producer == "ios_arm64") ||
            (consumer == "tvos_simulator_arm64" && producer == "ios_simulator_arm64") ||
            (consumer == "tvos_x64" && producer == "ios_x64")) {
            details.compatible()
        }
    }
}

// Prefer exact match; fall back to ios_arm64 only when tvos_arm64 is absent.
abstract class TvosIosDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        val consumer = details.consumerValue ?: return
        val candidates = details.candidateValues
        // Exact match always wins — this handles project-local deps that publish tvos_arm64.
        if (candidates.contains(consumer)) {
            details.closestMatch(consumer)
            return
        }
        // No exact match: fall back to ios equivalent for Compose klib-only libraries.
        val fallback = when (consumer) {
            "tvos_arm64" -> "ios_arm64"
            "tvos_simulator_arm64" -> "ios_simulator_arm64"
            "tvos_x64" -> "ios_x64"
            else -> return
        }
        if (candidates.contains(fallback)) {
            details.closestMatch(fallback)
        }
    }
}

android {
    namespace = "com.smellouk.kamper.ui"
    // compileSdk, minSdk, compileOptions provided by kamper.android.config

    testOptions {
        unitTests {
            // Prevents RuntimeException("Method not mocked") when JVM unit tests call
            // Android framework methods (e.g., SystemClock.elapsedRealtimeNanos()).
            isReturnDefaultValues = true
        }
    }
}


kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":kamper:engine"))
            implementation(project(":kamper:modules:cpu"))
            implementation(project(":kamper:modules:fps"))
            implementation(project(":kamper:modules:memory"))
            implementation(project(":kamper:modules:network"))
            implementation(project(":kamper:modules:issues"))
            implementation(project(":kamper:modules:jank"))
            implementation(project(":kamper:modules:gc"))
            implementation(project(":kamper:modules:thermal"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
        }
        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidUnitTest").dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}
