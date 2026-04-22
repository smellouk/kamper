Pod::Spec.new do |s|
  s.name         = 'KamperNative'
  s.version      = '1.0.0'
  s.summary      = 'Kamper performance monitoring native module for React Native'
  s.homepage     = 'https://github.com/smellouk/kamper'
  s.license      = { :type => 'Apache-2.0' }
  s.author       = { 'smellouk' => 'smellouk@example.com' }
  s.platform     = :ios, '14.0'
  s.source       = { :path => '.' }
  s.source_files = '*.{h,m,mm}'

  # Kamper XCFramework (built by Gradle — see pre_install in Podfile)
  s.vendored_frameworks = '../../../../kamper/xcframework/build/XCFrameworks/release/Kamper.xcframework'

  s.dependency 'React-Core'

  # Rebuild Kamper XCFramework when Kotlin sources change
  s.script_phases = [{
    :name              => 'Build Kamper XCFramework',
    :script            => %q{
cd "$PODS_ROOT/../../../.." && ./gradlew :kamper:xcframework:assembleKamperReleaseXCFramework
},
    :execution_position => :before_compile,
    :output_files       => ['$(PODS_ROOT)/../../../../kamper/xcframework/build/XCFrameworks/release/Kamper.xcframework/Info.plist']
  }]
end
