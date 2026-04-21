object Modules {
    const val ENGINE = ":kamper:engine"
    const val API = ":kamper:api"

    object Performances {
        const val CPU = ":kamper:modules:cpu"
        const val FPS = ":kamper:modules:fps"
        const val MEMORY = ":kamper:modules:memory"
        const val NETWORK = ":kamper:modules:network"
        const val ISSUES = ":kamper:modules:issues"
    }

    object Ui {
        const val ANDROID = ":kamper:ui:android"
    }

    const val XCFRAMEWORK = ":kamper:xcframework"

    object Samples {
        const val JVM = ":samples:jvm"
        const val MACOS = ":samples:macos"
        const val IOS = ":samples:ios"
        const val WEB = ":samples:web"
        const val COMPOSE = ":samples:compose"
    }
}