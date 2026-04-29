package com.smellouk.kamper.rn

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

/**
 * BaseReactPackage registration for KamperTurboModule.
 *
 * Per RESEARCH.md: BaseReactPackage (NOT legacy ReactPackage) is required
 * for New Architecture autolinking. ReactPackage.createNativeModules() is
 * not called in bridgeless mode.
 *
 * isTurboModule = true is REQUIRED — without it, the New Arch runtime
 * skips this module.
 */
class KamperTurboPackage : BaseReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        if (name == KamperTurboModule.NAME) {
            KamperTurboModule(reactContext)
        } else {
            null
        }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
        ReactModuleInfoProvider {
            mapOf(
                KamperTurboModule.NAME to ReactModuleInfo(
                    KamperTurboModule.NAME,             // name
                    KamperTurboModule.NAME,             // className (used for logging)
                    false,                              // canOverrideExistingModule
                    false,                              // needsEagerInit
                    false,                              // isCxxModule
                    true                                // isTurboModule — CRITICAL for New Arch
                )
            )
        }
}
