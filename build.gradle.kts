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
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath(Libs.Plugins.kotlin)
        classpath(Libs.Plugins.mokkery)
        classpath(Libs.Plugins.test_logger)
        classpath(Libs.Plugins.Compose.classpath)
    }
}

allprojects {
    if (project.path != Modules.Samples.WEB && project.path != Modules.Samples.COMPOSE) {
        apply(plugin = "com.adarshr.test-logger")
    }

    repositories {
        google()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Config.jvmTarget))
    }

    if (project.path != Modules.Samples.WEB && project.path != Modules.Samples.COMPOSE) {
        testLoggerConfig {
            theme = ThemeType.MOCHA
        }
    }
}

subprojects {
    afterEvaluate {
        if (project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform") === null) return@afterEvaluate
        kmmConfig {
            sourceSets["commonTest"].dependencies {
                implementation(kotlin(Libs.Kmm.Tests.kcommon))
                implementation(kotlin(Libs.Kmm.Tests.kannotationscommon))
                implementation(Libs.Kmm.Tests.coroutines)
            }

            sourceSets.findByName("androidUnitTest")?.dependencies {
                implementation(kotlin(Libs.Android.Tests.kotlin_junit))
            }

            sourceSets.findByName("androidInstrumentedTest")?.dependencies {
                implementation(Libs.Android.Tests.mockk)
            }

            sourceSets.findByName("jvmTest")?.dependencies {
                implementation(kotlin("test"))
            }

            sourceSets.findByName("jsTest")?.dependencies {
                implementation(kotlin("test"))
            }
        }

        if (project.plugins.findPlugin("com.android.library") != null) {
            androidConfig {
                compileSdk = Config.compileSdk
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
            required.set(true)
            outputLocation.set(file("build/reports/detekt.html"))
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

tasks.withType<Delete>().configureEach {
    if (name == "clean") delete(rootProject.layout.buildDirectory)
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
