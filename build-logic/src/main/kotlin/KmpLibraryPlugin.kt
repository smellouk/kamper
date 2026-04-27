import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType

/**
 * Convention plugin that bundles the Kamper KMP library boilerplate that is otherwise
 * repeated identically across all 8 publishing modules (engine + cpu/fps/memory/network/jank/gc/thermal/issues)
 * plus the api module.
 *
 * Applied as: id("kamper.kmp.library") in the module's plugins{} block.
 *
 * Behavior:
 *   1. Applies the three external plugin IDs by string (the shorthand form is
 *      NOT available inside Plugin<Project>):
 *        - org.jetbrains.kotlin.multiplatform
 *        - com.android.library
 *        - dev.mokkery
 *   2. Applies AndroidConfigPlugin so consumers don't need a separate id("kamper.android.config").
 *   3. Configures the KotlinMultiplatformExtension with all 10 Kamper targets:
 *        androidTarget, jvm, macosX64, macosArm64, iosArm64, iosSimulatorArm64,
 *        tvosArm64, tvosSimulatorArm64, js(IR).browser(), wasmJs.browser()
 *   4. Injects the standard commonTest / androidUnitTest / androidInstrumentedTest /
 *      jvmTest / jsTest dependencies (currently in root subprojects{}).
 *
 * What is INTENTIONALLY NOT included (Pitfall 8 / Critical Finding #3):
 *   - androidTarget { publishLibraryVariants("release") } — modules that publish must
 *     declare this themselves. The api module is a KMP library but does NOT publish, so
 *     including it here would force publishLibraryVariants on api too.
 *   - The id("kamper.publish") plugin — only publishing modules apply it.
 *
 * Modules that CANNOT use this plugin (Critical Finding #2):
 *   - kamper/xcframework — applies only org.jetbrains.kotlin.multiplatform; no com.android.library, no mokkery.
 *   - kamper/ui/android — uses only 3 targets and Compose plugins.
 *   - All demos modules — use com.android.application or stand-alone JVM/JS plugins.
 *   These are handled in Plans 03/04 by manual plugin application.
 */
class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 1) Apply the three external plugin IDs by full string
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.android.library")
        project.pluginManager.apply("dev.mokkery")

        // 2) Apply AndroidConfigPlugin (compileSdk=35, minSdk=21, Java 17, jvmTarget=17)
        project.pluginManager.apply(AndroidConfigPlugin::class.java)

        // 3) Configure all 10 KMP targets + 5 test source-set dep blocks
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            androidTarget()
            jvm()
            macosX64()
            macosArm64()
            iosArm64()
            iosSimulatorArm64()
            tvosArm64()
            tvosSimulatorArm64()
            js(KotlinJsCompilerType.IR) { browser() }
            wasmJs { browser() }

            // commonTest deps (root build.gradle.kts lines 74-78)
            sourceSets.getByName("commonTest").dependencies {
                implementation(project.dependencies.kotlin("test-common"))
                implementation(project.dependencies.kotlin("test-annotations-common"))
                implementation(
                    project.dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                )
            }

            // androidUnitTest deps (root build.gradle.kts lines 81-83)
            sourceSets.findByName("androidUnitTest")?.dependencies {
                implementation(project.dependencies.kotlin("test-junit"))
            }

            // androidInstrumentedTest deps (root build.gradle.kts lines 85-87)
            sourceSets.findByName("androidInstrumentedTest")?.dependencies {
                implementation(project.dependencies.create("io.mockk:mockk:1.14.5"))
            }

            // jvmTest deps (root build.gradle.kts lines 89-91)
            sourceSets.findByName("jvmTest")?.dependencies {
                implementation(project.dependencies.kotlin("test"))
            }

            // jsTest deps (root build.gradle.kts lines 93-95)
            sourceSets.findByName("jsTest")?.dependencies {
                implementation(project.dependencies.kotlin("test"))
            }
        }
    }
}
