plugins {
    `java-platform`
    id("konitor.publish")
}

// Read version from gradle.properties so BOM constraints carry an explicit version
// at configuration time (before KonitorPublishPlugin's afterEvaluate runs).
val bomVersion: String = project.rootProject.findProperty("VERSION_NAME")?.toString()
    ?: error("VERSION_NAME not found in gradle.properties")

dependencies {
    constraints {
        api("com.smellouk.konitor:engine:$bomVersion")
        api("com.smellouk.konitor:cpu-module:$bomVersion")
        api("com.smellouk.konitor:fps-module:$bomVersion")
        api("com.smellouk.konitor:memory-module:$bomVersion")
        api("com.smellouk.konitor:network-module:$bomVersion")
        api("com.smellouk.konitor:issues-module:$bomVersion")
        api("com.smellouk.konitor:jank-module:$bomVersion")
        api("com.smellouk.konitor:gc-module:$bomVersion")
        api("com.smellouk.konitor:thermal-module:$bomVersion")
    }
}
