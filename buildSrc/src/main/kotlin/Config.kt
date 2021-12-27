import org.gradle.api.JavaVersion

object Config {
    const val compileSdk = 30
    const val minSdk = 16
    const val targetSdk = 30
    const val versionCode = 1
    const val versionName = "1.0"
    val sourceCompatibility = JavaVersion.VERSION_11
    val targetCompatibility = JavaVersion.VERSION_11
    const val jvmTarget = "11"
}