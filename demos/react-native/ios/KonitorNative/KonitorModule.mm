#import "KonitorModule.h"
#import <Konitor/Konitor.h>

@implementation KonitorModule {
    KonitorKonitorBridge *_konitorBridge;
}

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents {
    return @[@"konitor_cpu", @"konitor_fps", @"konitor_memory", @"konitor_network"];
}

- (void)startObserving {}
- (void)stopObserving {}

RCT_EXPORT_METHOD(start) {
    if (_konitorBridge) return;
    _konitorBridge = [[KonitorKonitorBridge alloc] init];
    __weak typeof(self) weakSelf = self;

    [_konitorBridge
        setupOnCpu:^(KonitorCpuInfo *info) {
            if ([info isEqual:KonitorCpuInfoCompanion.shared.INVALID]) return;
            [weakSelf sendEventWithName:@"konitor_cpu" body:@{
                @"totalUseRatio": @(info.totalUseRatio),
                @"appRatio":      @(info.appRatio),
                @"userRatio":     @(info.userRatio),
                @"systemRatio":   @(info.systemRatio),
                @"ioWaitRatio":   @(info.ioWaitRatio)
            }];
        }
        onFps:^(KonitorFpsInfo *info) {
            if ([info isEqual:KonitorFpsInfoCompanion.shared.INVALID]) return;
            [weakSelf sendEventWithName:@"konitor_fps" body:@{
                @"fps": @(info.fps)
            }];
        }
        onMemory:^(KonitorMemoryInfo *info) {
            if ([info isEqual:KonitorMemoryInfoCompanion.shared.INVALID]) return;
            KonitorMemoryInfoHeapMemoryInfo *heap = info.heapMemoryInfo;
            KonitorMemoryInfoRamInfo *ram = info.ramInfo;
            [weakSelf sendEventWithName:@"konitor_memory" body:@{
                @"heapAllocatedMb": @(heap.allocatedInMb),
                @"heapMaxMb":       @(heap.maxMemoryInMb),
                @"ramUsedMb":       @(ram.totalRamInMb - ram.availableRamInMb),
                @"ramTotalMb":      @(ram.totalRamInMb),
                @"isLowMemory":     @(ram.isLowMemory)
            }];
        }
        onNetwork:^(KonitorNetworkInfo *info) {
            if ([info isEqual:KonitorNetworkInfoCompanion.shared.INVALID] ||
                [info isEqual:KonitorNetworkInfoCompanion.shared.NOT_SUPPORTED]) return;
            [weakSelf sendEventWithName:@"konitor_network" body:@{
                @"rxMb": @(info.rxSystemTotalInMb),
                @"txMb": @(info.txSystemTotalInMb)
            }];
        }
    ];

    [_konitorBridge start];
}

RCT_EXPORT_METHOD(stop) {
    [_konitorBridge stop];
    [_konitorBridge clear];
    _konitorBridge = nil;
}

RCT_EXPORT_METHOD(beginSpan:(NSString *)label thresholdMs:(double)thresholdMs) {}
RCT_EXPORT_METHOD(endSpan:(NSString *)label) {}
RCT_EXPORT_METHOD(logEvent:(NSString *)name) {}

RCT_EXPORT_METHOD(addListener:(NSString *)eventName) {}
RCT_EXPORT_METHOD(removeListeners:(double)count) {}

@end
