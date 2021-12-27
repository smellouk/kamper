private object Versions {
    const val kotlin = "1.6.10"
    const val coroutines = "1.6.0"
    const val androidx_app_compat = "1.3.1"
    const val androidx_annotations = "1.3.0"
    const val androidx_lifecycle = "2.3.1"
    const val androidx_material = "1.4.0"
    const val androidx_constraintlayout = "2.1.1"
    const val androidx_core_ktx = "1.6.0"
    const val mockk = "1.12.0"
}

object Libs {
    object Plugins {
        const val kotlin =
            "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    }

    object Kmm {
        val stdlib =
            "org.jetbrains.kotlin:kotlin-stdlib-common:${Versions.kotlin}"
        object Coroutines {
            const val core =
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        }

        object Tests {
            const val kcommon =
                "test-common"
            const val kannotationscommon =
                "test-annotations-common"
            const val mockk =
                "io.mockk:mockk-common:${Versions.mockk}"
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
    }
}