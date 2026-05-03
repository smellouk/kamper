// KonitorTurboModule.h
// Public header — declares the TurboModule interface inheriting from
// Codegen-generated NativeKonitorModuleSpecBase + conforming to NativeKonitorModuleSpec.
//
// Per D-04 / RESEARCH.md: the legacy bridge module / event emitter pattern
// is REPLACED by NativeKonitorModuleSpecBase (provided by Codegen via
// install_modules_dependencies in the podspec).

#import <NativeKonitorModuleSpec/NativeKonitorModuleSpec.h>

NS_ASSUME_NONNULL_BEGIN

@interface KonitorTurboModule : NativeKonitorModuleSpecBase <NativeKonitorModuleSpec>
@end

NS_ASSUME_NONNULL_END
