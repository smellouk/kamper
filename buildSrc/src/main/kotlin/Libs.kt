object Versions {
    const val kotlin = "2.3.20"
    const val coroutines = "1.10.1"
    const val compose = "1.8.0"

    const val androidx_app_compat = "1.7.0"
    const val androidx_annotations = "1.9.1"
    const val androidx_lifecycle = "2.8.7"
    const val androidx_material = "1.12.0"
    const val androidx_constraintlayout = "2.2.1"
    const val androidx_core_ktx = "1.16.0"

    const val mokkery = "3.3.0"
    const val mockk = "1.14.3"
    const val detekt = "1.23.7"

    const val gradle_test_logger = "4.0.0"
}

object Libs {
    object Plugins {
        const val kotlin =
            "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"

        const val mokkery =
            "dev.mokkery:dev.mokkery.gradle.plugin:${Versions.mokkery}"

        object Compose {
            const val id = "org.jetbrains.compose"
            const val kotlinPluginId = "org.jetbrains.kotlin.plugin.compose"
            const val classpath = "org.jetbrains.compose:compose-gradle-plugin:${Versions.compose}"
        }

        object Detekt {
            const val id =
                "io.gitlab.arturbosch.detekt"
            const val formatting =
                "io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.detekt}"
        }

        const val test_logger =
            "com.adarshr:gradle-test-logger-plugin:${Versions.gradle_test_logger}"
    }

    object Kmm {
        object Coroutines {
            const val core =
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        }

        object Tests {
            const val kcommon =
                "test-common"
            const val kannotationscommon =
                "test-annotations-common"
            const val coroutines =
                "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
        }
    }

    object Android {
        object Androidx {
            const val appCompat =
                "androidx.appcompat:appcompat:${Versions.androidx_app_compat}"
            const val annotationx =
                "androidx.annotation:annotation:${Versions.androidx_annotations}"
            const val lifecycleCommon =
                "androidx.lifecycle:lifecycle-common:${Versions.androidx_lifecycle}"
            const val material =
                "com.google.android.material:material:${Versions.androidx_material}"
            const val constraintlayout =
                "androidx.constraintlayout:constraintlayout:${Versions.androidx_constraintlayout}"
            const val core_ktx =
                "androidx.core:core-ktx:${Versions.androidx_core_ktx}"
        }

        object Coroutines {
            const val android =
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        }

        object Tests {
            const val kotlin_junit =
                "test-junit"
            const val mockk =
                "io.mockk:mockk:${Versions.mockk}"
        }
    }
}
