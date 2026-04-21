plugins {
    kotlin("multiplatform")
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
                implementation(project(Modules.ENGINE))
                implementation(project(Modules.Performances.CPU))
                implementation(project(Modules.Performances.FPS))
                implementation(project(Modules.Performances.MEMORY))
                implementation(project(Modules.Performances.NETWORK))
                implementation(project(Modules.Performances.ISSUES))
            }
        }
    }
}
