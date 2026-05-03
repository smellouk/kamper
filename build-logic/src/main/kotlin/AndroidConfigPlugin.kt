import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AndroidConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val compileSdkVersion = project.providers.gradleProperty("android.compileSdk").get().toInt()
        val minSdkVersion = project.providers.gradleProperty("android.minSdk").get().toInt()

        project.plugins.withId("com.android.library") {
            project.extensions.configure(LibraryExtension::class.java) {
                compileSdk = compileSdkVersion
                defaultConfig.minSdk = minSdkVersion
                compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                compileOptions.targetCompatibility = JavaVersion.VERSION_17
                testOptions {
                    unitTests.isReturnDefaultValues = true
                }
            }
        }

        project.tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget("17"))
        }
    }
}
