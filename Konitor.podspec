Pod::Spec.new do |s|
  s.name = 'Konitor'
  # x-release-please-version
  s.version      = '1.0.0'
  s.summary      = 'Kotlin Multiplatform performance monitoring library'
  s.description  = <<-DESC
    Konitor is a small Kotlin Multiplatform library that provides
    performance monitoring (CPU, FPS, memory, network) for your app.
  DESC
  s.homepage     = 'https://github.com/smellouk/konitor'
  s.license      = { :type => 'Apache-2.0', :file => 'LICENSE.txt' }
  s.author       = { 'Sidali Mellouk' => 'sidali.mellouk@zattoo.com' }
  s.platform     = :ios, '14.0'
  s.source       = {
    :http => "https://github.com/smellouk/konitor/releases/download/v#{s.version}/Konitor.xcframework.zip"
  }
  s.vendored_frameworks = 'Konitor.xcframework'
end
