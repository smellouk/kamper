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
    implementation(project(":libs:engine"))
    implementation(project(":libs:modules:cpu"))
    implementation(project(":libs:modules:fps"))
    implementation(project(":libs:modules:gc"))
    implementation(project(":libs:modules:gpu"))
    implementation(project(":libs:modules:issues"))
    implementation(project(":libs:modules:jank"))
    implementation(project(":libs:modules:memory"))
    implementation(project(":libs:modules:network"))
    implementation(project(":libs:modules:thermal"))
}
