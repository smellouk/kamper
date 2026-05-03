internal object Env {
    val CI: Boolean get() = System.getenv("CI") == "true"
}
