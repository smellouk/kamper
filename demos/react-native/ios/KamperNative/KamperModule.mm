#import "KamperModule.h"
#import <Kamper/Kamper.h>

@implementation KamperModule {
    KamperKamperBridge *_kamperBridge;
}

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents {
    return @[@"kamper_cpu", @"kamper_fps", @"kamper_memory", @"kamper_network"];
}

- (void)startObserving {}
- (void)stopObserving {}

RCT_EXPORT_METHOD(start) {
    if (_kamperBridge) return;
    _kamperBridge = [[KamperKamperBridge alloc] init];
    __weak typeof(self) weakSelf = self;

    [_kamperBridge
        setupOnCpu:^(KamperCpuInfo *info) {
            if ([info isEqual:KamperCpuInfoCompanion.shared.INVALID]) return;
            [weakSelf sendEventWithName:@"kamper_cpu" body:@{
                @"totalUseRatio": @(info.totalUseRatio),
                @"appRatio":      @(info.appRatio),
                @"userRatio":     @(info.userRatio),
                @"systemRatio":   @(info.systemRatio),
                @"ioWaitRatio":   @(info.ioWaitRatio)
            }];
        }
        onFps:^(KamperFpsInfo *info) {
            if ([info isEqual:KamperFpsInfoCompanion.shared.INVALID]) return;
            [weakSelf sendEventWithName:@"kamper_fps" body:@{
                @"fps": @(info.fps)
            }];
        }
        onMemory:^(KamperMemoryInfo *info) {
            if ([info isEqual:KamperMemoryInfoCompanion.shared.INVALID]) return;
            KamperMemoryInfoHeapMemoryInfo *heap = info.heapMemoryInfo;
            KamperMemoryInfoRamInfo *ram = info.ramInfo;
            [weakSelf sendEventWithName:@"kamper_memory" body:@{
                @"heapAllocatedMb": @(heap.allocatedInMb),
                @"heapMaxMb":       @(heap.maxMemoryInMb),
                @"ramUsedMb":       @(ram.totalRamInMb - ram.availableRamInMb),
                @"ramTotalMb":      @(ram.totalRamInMb),
                @"isLowMemory":     @(ram.isLowMemory)
            }];
        }
        onNetwork:^(KamperNetworkInfo *info) {
            if ([info isEqual:KamperNetworkInfoCompanion.shared.INVALID] ||
                [info isEqual:KamperNetworkInfoCompanion.shared.NOT_SUPPORTED]) return;
            [weakSelf sendEventWithName:@"kamper_network" body:@{
                @"rxMb": @(info.rxSystemTotalInMb),
                @"txMb": @(info.txSystemTotalInMb)
            }];
        }
    ];

    [_kamperBridge start];
}

RCT_EXPORT_METHOD(stop) {
    [_kamperBridge stop];
    [_kamperBridge clear];
    _kamperBridge = nil;
}

RCT_EXPORT_METHOD(addListener:(NSString *)eventName) {}
RCT_EXPORT_METHOD(removeListeners:(double)count) {}

@end
