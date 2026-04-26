plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.mokkery.gradle.plugin)
}

// Gradle 8.13 ships with embedded Kotlin 2.0.21. The compileOnly classpath
// (kotlin-gradle-plugin 2.3.20, mokkery 3.3.0) was compiled with Kotlin 2.3.x
// which produces metadata the embedded compiler rejects unless we suppress the check.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xskip-metadata-version-check")
    }
}

gradlePlugin {
    plugins {
        register("kamperKmpLibrary") {
            id = "kamper.kmp.library"
            implementationClass = "KmpLibraryPlugin"
        }
        register("kamperAndroidConfig") {
            id = "kamper.android.config"
            implementationClass = "AndroidConfigPlugin"
        }
        register("kamperPublish") {
            id = "kamper.publish"
            implementationClass = "KamperPublishPlugin"
        }
    }
}
