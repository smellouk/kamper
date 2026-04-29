// KamperTurboModule.h
// Public header — declares the TurboModule interface inheriting from
// Codegen-generated NativeKamperModuleSpecBase + conforming to NativeKamperModuleSpec.
//
// Per D-04 / RESEARCH.md: the legacy bridge module / event emitter pattern
// is REPLACED by NativeKamperModuleSpecBase (provided by Codegen via
// install_modules_dependencies in the podspec).

#import <NativeKamperModuleSpec/NativeKamperModuleSpec.h>

NS_ASSUME_NONNULL_BEGIN

@interface KamperTurboModule : NativeKamperModuleSpecBase <NativeKamperModuleSpec>
@end

NS_ASSUME_NONNULL_END
