import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import io.gitlab.arturbosch.detekt.Detekt
import java.lang.System.getenv as Env

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.test.logger) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.mokkery) apply false
}

extra.apply {
    val propertiesFile = rootDir.resolve("credentials.properties")
    when {
        propertiesFile.exists() -> {
            val properties = java.util.Properties()
            properties.load(propertiesFile.inputStream())

            set("KAMPER_GH_USER", properties["kamperGhUser"])
            set("KAMPER_GH_PAT", properties["kamperGhToken"])
        }
        Env().containsKey("KAMPER_GH_USER") && Env().containsKey("KAMPER_GH_PAT") -> {
            set("KAMPER_GH_USER", Env("KAMPER_GH_USER"))
            set("KAMPER_GH_PAT", Env("KAMPER_GH_PAT"))
        }
        else -> {
            set("KAMPER_GH_USER", "")
            set("KAMPER_GH_PAT", "")
        }
    }

    set("LIB_VERSION_NAME", generateVersionName())
}

allprojects {
    if (project.path != ":demos:web" && project.path != ":demos:compose") {
        apply(plugin = "com.adarshr.test-logger")
        testLoggerConfig {
            theme = ThemeType.MOCHA
        }
    }
}

tasks.named<Detekt>("detekt") {
    description = "Runs a custom detekt build on all project modules"
    setSource(files("$projectDir"))
    config.setFrom(files("$rootDir/quality/code/detekt.yml"))
    reports {
        html {
            required.set(true)
            outputLocation.set(file("build/reports/detekt.html"))
        }
    }
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/")
    exclude("**/build/")
    exclude("**/console/")
    exclude("**/android/")
    exclude("**/demos/")

    dependencies {
        detektPlugins(libs.detekt.formatting)
    }
}

tasks.withType<Delete>().configureEach {
    if (name == "clean") delete(rootProject.layout.buildDirectory)
}

fun Project.testLoggerConfig(configure: Action<TestLoggerExtension>) =
    (this as ExtensionAware).extensions.configure("testlogger", configure)

fun generateVersionName(): String {
    val default = "git describe --tags --abbrev=0".execute() ?: "0.1.0"
    val isRelease = Env().containsKey("CI")

    return if (isRelease) {
        default
    } else {
        val hash = "git rev-parse --short HEAD".execute()
        "$default-snapshot-$hash"
    }
}

fun String.execute(currentWorkingDir: File = file("./")): String? = try {
    providers.exec {
        workingDir(currentWorkingDir)
        commandLine(this@execute.split("\\s".toRegex()))
    }.standardOutput.asText.get().trim()
} catch (e: Exception) {
    null
}
