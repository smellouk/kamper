// KamperTurboModule.mm
// ObjC++ implementation of NativeKamperModuleSpec — TurboModule for iOS.
//
// iOS scope: Kamper.xcframework exports engine + cpu/fps/memory/network + KamperUi.
// onIssue/onJank/onGc/onThermal/onGpu never fire on iOS.
// onUserEvent IS supported — logEvent/startEvent/endEvent delegate to
// KamperKamper.shared and emit directly to JS.
// showOverlay/hideOverlay delegate to KamperKamperUi.shared (DEBUG only).
// JS-side metrics (jsMemory, jsGc, crash, spans) are stubs only.

#import "KamperTurboModule.h"
#import <Kamper/Kamper.h>
#include <time.h>

static BOOL boolFlag(NSDictionary *config, NSString *key) {
    id val = config[key];
    return (val == nil || [val boolValue]);
}

static int64_t monotonicNs(void) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

@implementation KamperTurboModule {
    KamperKamperBridge *_bridge;
    NSMutableDictionary<NSNumber *, KamperEventToken *> *_tokenMap;
    int _nextTokenId;
}

- (instancetype)init {
    if (self = [super init]) {
        _tokenMap = [NSMutableDictionary dictionary];
        _nextTokenId = 0;
    }
    return self;
}

- (void)start:(NSDictionary *)config {
    if (_bridge) return;

    BOOL cpuEnabled     = boolFlag(config, @"cpu");
    BOOL fpsEnabled     = boolFlag(config, @"fps");
    BOOL memoryEnabled  = boolFlag(config, @"memory");
    BOOL networkEnabled = boolFlag(config, @"network");

    _bridge = [[KamperKamperBridge alloc] init];
    __weak KamperTurboModule *ws = self;

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
            [ws emitOnFps:@{@"fps": @(info.fps)}];
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

- (void)stop {
    [_bridge stop]; [_bridge clear]; _bridge = nil;
    [_tokenMap removeAllObjects];
}

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

// JS-side metrics — no native backing on iOS, stubs required by the spec.
- (void)reportJsMemory:(double)usedMb totalMb:(double)totalMb {}
- (void)reportJsGc:(double)count pauseMs:(double)pauseMs {}
- (void)reportCrash:(NSString *)message stack:(NSString *)stack isFatal:(BOOL)isFatal {}
- (void)beginSpan:(NSString *)label thresholdMs:(double)thresholdMs {}
- (void)endSpan:(NSString *)label {}

- (void)logEvent:(NSString *)name {
    [KamperKamper.shared logEventName:name];
    __weak KamperTurboModule *ws = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ws emitOnUserEvent:@{@"name": name}];
    });
}

- (NSNumber *)startEvent:(NSString *)name {
    KamperEventToken *token = [KamperKamper.shared startEventName:name];
    NSNumber *tid = @(++_nextTokenId);
    _tokenMap[tid] = token;
    return tid;
}

- (void)endEvent:(double)tokenId {
    NSNumber *tid = @((int)tokenId);
    KamperEventToken *token = _tokenMap[tid];
    if (!token) return;
    [_tokenMap removeObjectForKey:tid];
    int64_t nowNs = monotonicNs();
    [KamperKamper.shared endEventToken:token];
    int64_t durationMs = (nowNs - token.startNs) / 1000000LL;
    NSString *name = token.name;
    __weak KamperTurboModule *ws = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ws emitOnUserEvent:@{@"name": name, @"durationMs": @(durationMs)}];
    });
}

- (std::shared_ptr<facebook::react::TurboModule>)
    getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeKamperModuleSpecJSI>(params);
}

+ (NSString *)moduleName {
    return @"KamperModule";
}

@end
