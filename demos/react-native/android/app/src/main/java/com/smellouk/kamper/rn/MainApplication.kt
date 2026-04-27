package com.smellouk.kamper.rn

import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import android.app.Application

/**
 * MainApplication wires the React Native runtime.
 *
 * Phase 12: replaces legacy `KamperPackage()` (in-app Legacy Bridge module —
 * still present at demos/.../KamperPackage.kt for reference but unused) with
 * the new `KamperTurboPackage()` from `react-native-kamper` (kamper/react-native/).
 *
 * Why explicit registration (revision iteration 1 fix):
 * Although `PackageList(this)` would auto-register `KamperTurboPackage` via
 * RN autolinking IF `npm install` populated `node_modules/react-native-kamper`,
 * relying on autolinking-only is fragile. If a developer runs `./gradlew :app:assembleDebug`
 * without first running `npm install`, the autolinker manifest is stale and the
 * module silently disappears from the package list. Explicit registration
 * via `.apply { add(KamperTurboPackage()) }` makes the wiring intentional and
 * compile-checked — if `react-native-kamper` is not installed, the build fails at
 * the `import com.smellouk.kamper.rn.KamperTurboPackage` line instead of at runtime.
 */
class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList = PackageList(this).packages.apply {
        // Explicit registration — defense against autolinker misses.
        // Duplicate-add is safe: BaseReactPackage.getModule() checks NAME equality
        // and the New Arch module registry de-duplicates by NAME ("KamperModule").
        add(com.smellouk.kamper.rn.KamperTurboPackage())
      },
    )
  }

  override fun onCreate() {
    super.onCreate()
    loadReactNative(this)
  }
}
