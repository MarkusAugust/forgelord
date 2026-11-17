package forgelord.page

/**
 * A CSS file from the classpath, as text.
 *
 * A component's CSS lives beside its Kotlin in `src/main/kotlin`, and the build puts it on the
 * classpath under the same path, such as `forgelord/components/shell/Shell.css`.
 */
fun stylesheet(path: String): String =
    Thread.currentThread().contextClassLoader.getResource(path)?.readText()
        ?: error("No stylesheet at $path on the classpath")
