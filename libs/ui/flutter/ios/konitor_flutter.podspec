Pod::Spec.new do |s|
  s.name             = 'konitor_flutter'
  s.version          = '0.1.0'
  s.summary          = 'Konitor performance monitoring plugin for Flutter (iOS)'
  s.description      = <<-DESC
    iOS bridge for Konitor performance monitoring engine.
    Exposes CPU, FPS, memory, and network metric streams to Flutter
    via FlutterEventChannel. Android-only modules (issues, jank, gc, thermal, gpu)
    are registered as no-op stream handlers.
  DESC
  s.homepage         = 'https://github.com/smellouk/konitor'
  s.license          = { :type => 'Apache-2.0' }
  s.author           = { 'smellouk' => 'sidali.mellouk@zattoo.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*.swift'
  s.ios.deployment_target = '14.0'

  # Symlink at ios/Konitor.xcframework → ../../../apple-sdk/build/XCFrameworks/release/Konitor.xcframework
  # Using a local symlink keeps the path simple and avoids CocoaPods' absolute-path rejection.
  s.vendored_frameworks = 'Konitor.xcframework'

  s.dependency 'Flutter'

  # Kotlin/Native XCFramework only ships arm64 slices.
  # Exclude x86_64 so the simulator build doesn't try a missing architecture.
  s.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  s.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }

  # Build XCFramework before this pod compiles.
  # When consumed from demos/flutter/ios/Pods, PODS_ROOT/../../../.. = repo root
  s.script_phases = [{
    :name               => 'Build Konitor XCFramework',
    :script             => %q{
set -e
REPO_ROOT="$PODS_ROOT/../../../.."
echo "Building Konitor.xcframework from $REPO_ROOT"
cd "$REPO_ROOT" && ./gradlew :libs:apple-sdk:assembleKonitorXCFramework
},
    :execution_position => :before_compile,
    :output_files       => ['$(PODS_ROOT)/../../../../libs/apple-sdk/build/XCFrameworks/release/Konitor.xcframework/Info.plist']
  }]
end
