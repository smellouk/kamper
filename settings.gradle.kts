rootProject.name = "Kamper"

include(":kamper:api")
include(":kamper:engine")
include(":kamper:xcframework")

include(":kamper:modules:cpu")
include(":kamper:modules:fps")
include(":kamper:modules:memory")
include(":kamper:modules:network")
include(":kamper:modules:issues")
include(":kamper:modules:jank")

include(":kamper:ui:android")

include(":samples:android")
include(":samples:jvm")
include(":samples:macos")
include(":samples:web")
include(":samples:compose")
include(":samples:ios")
