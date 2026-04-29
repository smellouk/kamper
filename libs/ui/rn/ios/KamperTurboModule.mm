// KamperTurboModule.mm
// ObjC++ implementation of NativeKamperModuleSpec — TurboModule for iOS.
//
// Ports demos/react-native/ios/KamperNative/KamperModule.mm (Legacy Bridge —
// event emitter + legacy export macros) to the New Architecture pattern.
//
// Per D-04: New Arch only.
// Per D-06: payload shapes preserved verbatim from KamperModule.mm.
// Per D-08: per-module config parsed from Codegen-generated SpecStartConfig.
// Per D-10/D-11: showOverlay/hideOverlay dispatch to main queue (UIKit thread rule).
//
// iOS scope (Pitfall 2 in 14-RESEARCH.md):
// Kamper.xcframework only exports cpu/fps/memory/network. The TurboModule spec
// declares 8 events but only 4 emit native data on iOS. The other 4
// (onIssue/onJank/onGc/onThermal) are Codegen-generated empty emitters that
// never fire on iOS — JS hooks for those metrics return null/empty arrays
// when running on iOS. This is documented in KamperConfig JSDoc.

#import "KamperTurboModule.h"
#import <Kamper/Kamper.h>

@implementation KamperTurboModule {
    KamperKamperBridge *_bridge;
}

// ─── Per-module config helper (D-08) ─────────────────────────────────────
// Codegen generates a C++ struct (SpecStartConfig) with optional<bool> fields
// matching the JS object keys. Read each flag with
// `if (auto opt = config.fieldName()) { return *opt; }` else default true.
// (Codegen field-accessor names: cpu(), fps(), memory(), network(), issues(),
// jank(), gc(), thermal() — matching JS KamperConfig keys verbatim.)

static BOOL flagOrTrue(folly::Optional<bool> opt) {
    return opt ? *opt : YES;
}

// ─── start(config) — wire 4 iOS-supported listeners ──────────────────────
- (void)start:(JS::NativeKamperModule::SpecStartConfig &)config {
    if (_bridge) return;

    BOOL cpuEnabled     = flagOrTrue(config.cpu());
    BOOL fpsEnabled     = flagOrTrue(config.fps());
    BOOL memoryEnabled  = flagOrTrue(config.memory());
    BOOL networkEnabled = flagOrTrue(config.network());
    // issues/jank/gc/thermal flags are read but ignored on iOS (XCFramework
    // doesn't export those modules — see file header comment).

    _bridge = [[KamperKamperBridge alloc] init];
    __weak typeof(self) ws = self;

    [_bridge
        setupOnCpu:^(KamperCpuInfo *info) {
            if (!cpuEnabled) return;
            if ([info isEqual:KamperCpuInfoCompanion.shared.INVALID]) return;
            [ws emitOnCpu:@{
                @"totalUseRatio": @(info.totalUseRatio),
                @"appRatio":      @(info.appRatio),
                @"userRatio":     @(info.userRatio),
                @"systemRatio":   @(info.systemRatio),
                @"ioWaitRatio":   @(info.ioWaitRatio)
            }];
        }
        onFps:^(KamperFpsInfo *info) {
            if (!fpsEnabled) return;
            if ([info isEqual:KamperFpsInfoCompanion.shared.INVALID]) return;
            [ws emitOnFps:@{
                @"fps": @(info.fps)
            }];
        }
        onMemory:^(KamperMemoryInfo *info) {
            if (!memoryEnabled) return;
            if ([info isEqual:KamperMemoryInfoCompanion.shared.INVALID]) return;
            KamperMemoryInfoHeapMemoryInfo *heap = info.heapMemoryInfo;
            KamperMemoryInfoRamInfo *ram = info.ramInfo;
            [ws emitOnMemory:@{
                @"heapAllocatedMb": @(heap.allocatedInMb),
                @"heapMaxMb":       @(heap.maxMemoryInMb),
                @"ramUsedMb":       @(ram.totalRamInMb - ram.availableRamInMb),
                @"ramTotalMb":      @(ram.totalRamInMb),
                @"isLowMemory":     @(ram.isLowMemory)
            }];
        }
        onNetwork:^(KamperNetworkInfo *info) {
            if (!networkEnabled) return;
            // Double INVALID guard — established pattern from KamperModule.mm lines 52-53.
            if ([info isEqual:KamperNetworkInfoCompanion.shared.INVALID] ||
                [info isEqual:KamperNetworkInfoCompanion.shared.NOT_SUPPORTED]) return;
            [ws emitOnNetwork:@{
                @"rxMb": @(info.rxSystemTotalInMb),
                @"txMb": @(info.txSystemTotalInMb)
            }];
        }
    ];

    [_bridge start];
}

// ─── stop ────────────────────────────────────────────────────────────────
// Exact lifecycle pattern from KamperModule.mm lines 64-67.
- (void)stop {
    [_bridge stop]; [_bridge clear]; _bridge = nil;
}

// ─── Overlay (D-10, D-11) ────────────────────────────────────────────────
// Pitfall 3: KamperUi.attach()/detach() touch UIKit (UIWindow/UIApplication).
// MUST run on main queue — TurboModule methods may be invoked off the main
// thread.
//
// Kotlin/Native ObjC binding: `KamperUi.show()` (a member of `actual object KamperUi`
// in package com.smellouk.kamper.ui) is exposed as an instance method on
// `KamperKamperUi.shared` (Kotlin/Native module-name prefix `Kamper` + class
// `KamperUi` + `.shared` companion accessor for `object`).
//
// VERIFY at first build: if symbol not found, run `nm -gU
// kamper/xcframework/build/XCFrameworks/release/Kamper.xcframework/ios-arm64/Kamper.framework/Kamper
// | grep -i kamperui` to find the actual exported name. Likely alternatives:
//   - KamperKamperUi.shared show   <- primary attempt (used below)
//   - KamperKamperUiKt show
//   - KamperKamperUi show          (some KMP versions inline `.shared`)
// Adjust this file accordingly if the build error gives the correct symbol.

- (void)showOverlay {
#if DEBUG
    dispatch_async(dispatch_get_main_queue(), ^{
        [KamperKamperUi.shared show];
    });
#endif
}

- (void)hideOverlay {
#if DEBUG
    dispatch_async(dispatch_get_main_queue(), ^{
        [KamperKamperUi.shared hide];
    });
#endif
}

// ─── TurboModule JSI bridge (REQUIRED for New Arch) ──────────────────────
// Codegen autogenerates NativeKamperModuleSpecJSI — this method tells the
// RN runtime which JSI implementation class to instantiate.
- (std::shared_ptr<facebook::react::TurboModule>)
    getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeKamperModuleSpecJSI>(params);
}

// Module name — MUST match Android KamperTurboModule.NAME and JS
// TurboModuleRegistry.getEnforcing<Spec>('KamperModule').
+ (NSString *)moduleName {
    return @"KamperModule";
}

@end
