// KonitorTurboModule.mm
// ObjC++ implementation of NativeKonitorModuleSpec — TurboModule for iOS.
//
// iOS scope: Konitor.xcframework exports engine + cpu/fps/memory/network + KonitorUi.
// onIssue/onJank/onGc/onThermal/onGpu never fire on iOS.
// onUserEvent IS supported — logEvent/startEvent/endEvent delegate to
// KonitorKonitor.shared and emit directly to JS.
// showOverlay/hideOverlay delegate to KonitorKonitorUi.shared (DEBUG only).
// JS-side metrics (jsMemory, jsGc, crash, spans) are stubs only.

#import "KonitorTurboModule.h"
#import <Konitor/Konitor.h>
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

@implementation KonitorTurboModule {
    KonitorKonitorBridge *_bridge;
    NSMutableDictionary<NSNumber *, KonitorEventToken *> *_tokenMap;
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

    _bridge = [[KonitorKonitorBridge alloc] init];
    __weak KonitorTurboModule *ws = self;

    [_bridge
        setupOnCpu:^(KonitorCpuInfo *info) {
            if (!cpuEnabled) return;
            if ([info isEqual:KonitorCpuInfoCompanion.shared.INVALID]) return;
            [ws emitOnCpu:@{
                @"totalUseRatio": @(info.totalUseRatio),
                @"appRatio":      @(info.appRatio),
                @"userRatio":     @(info.userRatio),
                @"systemRatio":   @(info.systemRatio),
                @"ioWaitRatio":   @(info.ioWaitRatio)
            }];
        }
        onFps:^(KonitorFpsInfo *info) {
            if (!fpsEnabled) return;
            if ([info isEqual:KonitorFpsInfoCompanion.shared.INVALID]) return;
            [ws emitOnFps:@{@"fps": @(info.fps)}];
        }
        onMemory:^(KonitorMemoryInfo *info) {
            if (!memoryEnabled) return;
            if ([info isEqual:KonitorMemoryInfoCompanion.shared.INVALID]) return;
            KonitorMemoryInfoHeapMemoryInfo *heap = info.heapMemoryInfo;
            KonitorMemoryInfoRamInfo *ram = info.ramInfo;
            [ws emitOnMemory:@{
                @"heapAllocatedMb": @(heap.allocatedInMb),
                @"heapMaxMb":       @(heap.maxMemoryInMb),
                @"ramUsedMb":       @(ram.totalRamInMb - ram.availableRamInMb),
                @"ramTotalMb":      @(ram.totalRamInMb),
                @"isLowMemory":     @(ram.isLowMemory)
            }];
        }
        onNetwork:^(KonitorNetworkInfo *info) {
            if (!networkEnabled) return;
            if ([info isEqual:KonitorNetworkInfoCompanion.shared.INVALID] ||
                [info isEqual:KonitorNetworkInfoCompanion.shared.NOT_SUPPORTED]) return;
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
        [KonitorKonitorUi.shared show];
    });
#endif
}

- (void)hideOverlay {
#if DEBUG
    dispatch_async(dispatch_get_main_queue(), ^{
        [KonitorKonitorUi.shared hide];
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
    [KonitorKonitor.shared logEventName:name];
    __weak KonitorTurboModule *ws = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ws emitOnUserEvent:@{@"name": name}];
    });
}

- (NSNumber *)startEvent:(NSString *)name {
    KonitorEventToken *token = [KonitorKonitor.shared startEventName:name];
    NSNumber *tid = @(++_nextTokenId);
    _tokenMap[tid] = token;
    return tid;
}

- (void)endEvent:(double)tokenId {
    NSNumber *tid = @((int)tokenId);
    KonitorEventToken *token = _tokenMap[tid];
    if (!token) return;
    [_tokenMap removeObjectForKey:tid];
    int64_t nowNs = monotonicNs();
    [KonitorKonitor.shared endEventToken:token];
    int64_t durationMs = (nowNs - token.startNs) / 1000000LL;
    NSString *name = token.name;
    __weak KonitorTurboModule *ws = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ws emitOnUserEvent:@{@"name": name, @"durationMs": @(durationMs)}];
    });
}

- (std::shared_ptr<facebook::react::TurboModule>)
    getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeKonitorModuleSpecJSI>(params);
}

+ (NSString *)moduleName {
    return @"KonitorModule";
}

@end
