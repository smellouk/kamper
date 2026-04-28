pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS) is required because the Kotlin/JS toolchain
    // used by demos/web adds nodejs.org/dist as an ivy distribution repository at configuration
    // time via AbstractSetupTask.withUrlRepo(). Gradle's FAIL_ON_PROJECT_REPOS blocks ALL
    // project.repositories.add() calls unconditionally — pre-declaring the same URL in settings
    // does NOT satisfy the check. PREFER_SETTINGS ensures settings repositories (google, mavenCentral)
    // take precedence over project-level additions while still allowing KGP toolchain repos
    // declared here to be found.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Kotlin/JS NodeJS toolchain distribution (required by demos/web js(IR) target)
        // KGP artifact: org.nodejs:node:{version}, classifier: {os}-{arch}
        ivy {
            name = "Node Distributions"
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/node-v[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
        }
        // Kotlin/JS Yarn toolchain distribution (required by demos/web js(IR) browser target)
        // KGP artifact: com.yarnpkg:yarn:{version}
        ivy {
            name = "Yarn Distributions"
            url = uri("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout {
                artifact("v[revision]/yarn-v[revision].[ext]")
            }
            metadataSources { artifact() }
        }
    }
}

rootProject.name = "Kamper"

include(":kamper:api")
include(":kamper:bom")
include(":kamper:engine")
include(":kamper:xcframework")

include(":kamper:modules:cpu")
include(":kamper:modules:fps")
include(":kamper:modules:memory")
include(":kamper:modules:network")
include(":kamper:modules:issues")
include(":kamper:modules:jank")
include(":kamper:modules:gc")
include(":kamper:modules:thermal")

include(":kamper:integrations:sentry")
include(":kamper:integrations:firebase")
include(":kamper:integrations:opentelemetry")

include(":kamper:ui:kmm")
// :kamper:ui:rn is NOT included here: it imports com.facebook.react.* which requires
// react-android (only resolvable in the RN demo context via com.facebook.react.settings).
// It is compiled when building from demos/react-native/android, which composite-includes
// this root build and discovers the module via autolink.

include(":demos:android")
include(":demos:jvm")
include(":demos:macos")
include(":demos:web")
include(":demos:compose")
include(":demos:ios")

// React Native demo — integrated as composite build per D-09.
// Uses includeBuild (NOT a regular subproject include) because the RN demo has its own
// settings.gradle with com.facebook.react.settings applied. The Gradle project root for the
// RN demo is at demos/react-native/android/ (standard RN project layout). The RN demo's
// settings.gradle already has includeBuild('../../..') for dependency substitution back to
// the root — Gradle 8.x handles the mutual composite relationship correctly.
includeBuild("demos/react-native/android")
