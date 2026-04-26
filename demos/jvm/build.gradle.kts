plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.smellouk.kamper.jvm.MainKt")
}

tasks.register<JavaExec>("runConsole") {
    group = "application"
    description = "Run the JVM console monitor (CPU + Memory + Network, no GUI)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.smellouk.kamper.jvm.ConsoleMainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":kamper:engine"))
    implementation(project(":kamper:modules:cpu"))
    implementation(project(":kamper:modules:fps"))
    implementation(project(":kamper:modules:memory"))
    implementation(project(":kamper:modules:network"))
    implementation(project(":kamper:modules:issues"))
    implementation(project(":kamper:modules:jank"))
    implementation(project(":kamper:modules:gc"))
    implementation(project(":kamper:modules:thermal"))
}
