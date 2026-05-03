import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    alias(libs.plugins.kotlin.compose)
}

// KamperUi uses ios_arm64/ios_simulator_arm64 Compose klib variants; same compatibility
// shim as libs/ui/kmm so Gradle resolves the correct klib for each target.
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

abstract class TvosIosDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        val consumer = details.consumerValue ?: return
        val candidates = details.candidateValues
        if (candidates.contains(consumer)) { details.closestMatch(consumer); return }
        val fallback = when (consumer) {
            "tvos_arm64" -> "ios_arm64"
            "tvos_simulator_arm64" -> "ios_simulator_arm64"
            "tvos_x64" -> "ios_x64"
            else -> return
        }
        if (candidates.contains(fallback)) details.closestMatch(fallback)
    }
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
            export(project(":libs:ui:kmm"))
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
                api(project(":libs:ui:kmm"))
            }
        }
    }
}
