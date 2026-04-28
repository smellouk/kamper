Pod::Spec.new do |s|
  s.name         = 'Kamper'
  # x-release-please-version
  s.version      = '1.0.0'
  s.summary      = 'Kotlin Multiplatform performance monitoring library'
  s.description  = <<-DESC
    Kamper is a small Kotlin Multiplatform library that provides
    performance monitoring (CPU, FPS, memory, network) for your app.
  DESC
  s.homepage     = 'https://github.com/smellouk/kamper'
  s.license      = { :type => 'Apache-2.0', :file => 'LICENSE.txt' }
  s.author       = { 'Sidali Mellouk' => 'sidali.mellouk@zattoo.com' }
  s.platform     = :ios, '14.0'
  s.source       = {
    :http => "https://github.com/smellouk/kamper/releases/download/v#{s.version}/Kamper.xcframework.zip"
  }
  s.vendored_frameworks = 'Kamper.xcframework'
end
