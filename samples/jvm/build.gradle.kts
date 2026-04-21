plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.smellouk.kamper.samples.jvm.MainKt")
}

tasks.register<JavaExec>("runConsole") {
    group = "application"
    description = "Run the JVM console monitor (CPU + Memory + Network, no GUI)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.smellouk.kamper.samples.jvm.ConsoleMainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Config.jvmTarget))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(Modules.ENGINE))
    implementation(project(Modules.Performances.CPU))
    implementation(project(Modules.Performances.FPS))
    implementation(project(Modules.Performances.MEMORY))
    implementation(project(Modules.Performances.NETWORK))
    implementation(project(Modules.Performances.ISSUES))
}
