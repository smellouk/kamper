import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import com.android.build.gradle.LibraryExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.lang.System.getenv as Env

plugins {
    id(Libs.Plugins.Detekt.id) version Versions.detekt
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

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // keeping this here to allow automatic update
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath(Libs.Plugins.kotlin)
        classpath(Libs.Plugins.test_logger)
    }
}

allprojects {
    apply(plugin = "com.adarshr.test-logger")

    repositories {
        google()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = Config.jvmTarget
    }

    testLoggerConfig {
        theme = ThemeType.MOCHA
    }
}

subprojects {
    afterEvaluate {
        if (project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform") === null) return@afterEvaluate
        kmmConfig {
            sourceSets["commonTest"].dependencies {
                implementation(Libs.Kmm.Tests.mockk)
                implementation(kotlin(Libs.Kmm.Tests.kcommon))
                implementation(kotlin(Libs.Kmm.Tests.kannotationscommon))
            }

            sourceSets["androidTest"].dependencies {
                implementation(Libs.Android.Tests.mockk)
                implementation(kotlin(Libs.Android.Tests.kotlin_junit))
            }
        }

        if (project.plugins.findPlugin("com.android.library") != null) {
            androidConfig {
                compileSdk = Config.compileSdk
                sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
                defaultConfig {
                    minSdk = Config.minSdk
                    targetSdk = Config.targetSdk
                }
                compileOptions {
                    sourceCompatibility = Config.sourceCompatibility
                    targetCompatibility = Config.targetCompatibility
                }
            }
        }
    }
}

tasks.named<Detekt>("detekt") {
    description = "Runs a custom detekt build on all project modules"
    setSource(files("$projectDir"))
    config.setFrom(files("$rootDir/quality/code/detekt.yml"))
    reports {
        html {
            enabled = true
            destination = file("build/reports/detekt.html")
        }
    }
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/")
    exclude("**/build/")
    exclude("**/buildSrc/")
    exclude("**/console/")
    exclude("**/android/")
    exclude("**/samples/")

    dependencies {
        detektPlugins(Libs.Plugins.Detekt.formatting)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun Project.testLoggerConfig(configure: Action<TestLoggerExtension>) =
    (this as ExtensionAware).extensions.configure("testlogger", configure)

fun Project.kmmConfig(configure: Action<KotlinMultiplatformExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)

fun Project.androidConfig(configure: Action<LibraryExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

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
    val byteOut = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = this@execute.split("\\s".toRegex())
        standardOutput = byteOut
    }
    String(byteOut.toByteArray()).trim()
} catch (e: Exception) {
    null
}
