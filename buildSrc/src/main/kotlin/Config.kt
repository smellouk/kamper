import org.gradle.api.JavaVersion

object Config {
    const val compileSdk = 35
    const val minSdk = 21
    const val targetSdk = 35
    const val versionCode = 1
    const val versionName = "1.0"
    val sourceCompatibility = JavaVersion.VERSION_17
    val targetCompatibility = JavaVersion.VERSION_17
    const val jvmTarget = "17"
}