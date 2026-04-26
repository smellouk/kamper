import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Convention plugin that applies Kamper's Android SDK + Java 17 defaults.
 *
 * Configures every project that has the Android Library plugin applied:
 *   - compileSdk = 35              (Config.compileSdk)
 *   - defaultConfig.minSdk = 21    (Config.minSdk)
 *   - compileOptions.sourceCompatibility/targetCompatibility = JavaVersion.VERSION_17
 *
 * Configures every KotlinCompile task in the project:
 *   - compilerOptions.jvmTarget = "17"   (Config.jvmTarget)
 *
 * Note: targetSdk is intentionally OMITTED here. Module-level Android namespace and any
 * targetSdk override (e.g., demos that need targetSdk = 35) is set in the module's own
 * build.gradle.kts. KMP library modules inherit targetSdk = 35 implicitly via Gradle defaults
 * and the per-module android { } DSL.
 *
 * Applied automatically by KmpLibraryPlugin so KMP library modules don't need to list both.
 * Application modules (demos/android, demos/compose) hardcode SDK values directly because
 * they use ApplicationExtension, not LibraryExtension.
 */
class AndroidConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Configure LibraryExtension defaults (only takes effect once com.android.library is applied)
        project.plugins.withId("com.android.library") {
            project.extensions.configure(LibraryExtension::class.java) {
                compileSdk = 35
                defaultConfig.minSdk = 21
                compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                compileOptions.targetCompatibility = JavaVersion.VERSION_17
            }
        }

        // jvmTarget = "17" on every KotlinCompile task (mirrors root allprojects{} block)
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget("17"))
        }
    }
}
