package com.smellouk.kamper.rn

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class KamperPackage : ReactPackage {
    override fun createNativeModules(ctx: ReactApplicationContext) = listOf(KamperModule(ctx))
    override fun createViewManagers(ctx: ReactApplicationContext) = emptyList<ViewManager<*, *>>()
}
