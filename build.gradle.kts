import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Libs.Plugins.Detekt.id) version Versions.detekt
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
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = Config.jvmTarget
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
