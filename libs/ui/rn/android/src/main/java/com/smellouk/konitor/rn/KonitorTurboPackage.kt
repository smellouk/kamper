package com.smellouk.konitor.rn

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

/**
 * BaseReactPackage registration for KonitorTurboModule.
 *
 * Per RESEARCH.md: BaseReactPackage (NOT legacy ReactPackage) is required
 * for New Architecture autolinking. ReactPackage.createNativeModules() is
 * not called in bridgeless mode.
 *
 * isTurboModule = true is REQUIRED — without it, the New Arch runtime
 * skips this module.
 */
class KonitorTurboPackage : BaseReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        if (name == KonitorTurboModule.NAME) {
            KonitorTurboModule(reactContext)
        } else {
            null
        }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
        ReactModuleInfoProvider {
            mapOf(
                KonitorTurboModule.NAME to ReactModuleInfo(
                    KonitorTurboModule.NAME,             // name
                    KonitorTurboModule.NAME,             // className (used for logging)
                    false,                              // canOverrideExistingModule
                    false,                              // needsEagerInit
                    false,                              // isCxxModule
                    true                                // isTurboModule — CRITICAL for New Arch
                )
            )
        }
}
