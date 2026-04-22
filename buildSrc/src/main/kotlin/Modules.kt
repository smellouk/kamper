object Modules {
    const val ENGINE = ":kamper:engine"
    const val API = ":kamper:api"

    object Performances {
        const val CPU = ":kamper:modules:cpu"
        const val FPS = ":kamper:modules:fps"
        const val MEMORY = ":kamper:modules:memory"
        const val NETWORK = ":kamper:modules:network"
        const val ISSUES = ":kamper:modules:issues"
        const val JANK = ":kamper:modules:jank"
        const val GC = ":kamper:modules:gc"
        const val THERMAL = ":kamper:modules:thermal"
    }

    object Ui {
        const val ANDROID = ":kamper:ui:android"
    }

    const val XCFRAMEWORK = ":kamper:xcframework"

    object Demos {
        const val JVM = ":demos:jvm"
        const val MACOS = ":demos:macos"
        const val IOS = ":demos:ios"
        const val WEB = ":demos:web"
        const val COMPOSE = ":demos:compose"
    }
}