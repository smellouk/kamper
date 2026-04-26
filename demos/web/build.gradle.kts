plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "web.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
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
        }
    }
}
