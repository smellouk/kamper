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
include(":kamper:modules:gc")
include(":kamper:modules:thermal")

include(":kamper:ui:android")

include(":demos:android")
include(":demos:jvm")
include(":demos:macos")
include(":demos:web")
include(":demos:compose")
include(":demos:ios")
