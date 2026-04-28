plugins {
    `java-platform`
    id("kamper.publish")
}

// Read version from gradle.properties so BOM constraints carry an explicit version
// at configuration time (before KamperPublishPlugin's afterEvaluate runs).
val bomVersion: String = project.rootProject.findProperty("VERSION_NAME")?.toString()
    ?: error("VERSION_NAME not found in gradle.properties")

dependencies {
    constraints {
        api("com.smellouk.kamper:engine:$bomVersion")
        api("com.smellouk.kamper:cpu-module:$bomVersion")
        api("com.smellouk.kamper:fps-module:$bomVersion")
        api("com.smellouk.kamper:memory-module:$bomVersion")
        api("com.smellouk.kamper:network-module:$bomVersion")
        api("com.smellouk.kamper:issues-module:$bomVersion")
        api("com.smellouk.kamper:jank-module:$bomVersion")
        api("com.smellouk.kamper:gc-module:$bomVersion")
        api("com.smellouk.kamper:thermal-module:$bomVersion")
    }
}
