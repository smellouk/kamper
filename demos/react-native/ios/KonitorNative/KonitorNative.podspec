Pod::Spec.new do |s|
  s.name         = 'KonitorNative'
  s.version      = '1.0.0'
  s.summary      = 'Konitor performance monitoring native module for React Native'
  s.homepage     = 'https://github.com/smellouk/konitor'
  s.license      = { :type => 'Apache-2.0' }
  s.author       = { 'smellouk' => 'smellouk@example.com' }
  s.platform     = :ios, '14.0'
  s.source       = { :path => '.' }
  s.source_files = '*.{h,m,mm}'

  # Konitor XCFramework (built by Gradle — see pre_install in Podfile)
  s.vendored_frameworks = '../../../../libs/apple-sdk/build/XCFrameworks/release/Konitor.xcframework'

  s.dependency 'React-Core'

  # Rebuild Konitor XCFramework when Kotlin sources change
  s.script_phases = [{
    :name              => 'Build Konitor XCFramework',
    :script            => %q{
cd "$PODS_ROOT/../../../.." && ./gradlew :libs:apple-sdk:assembleKonitorXCFramework
},
    :execution_position => :before_compile,
    :output_files       => ['$(PODS_ROOT)/../../../../libs/apple-sdk/build/XCFrameworks/release/Konitor.xcframework/Info.plist']
  }]
end
