package forgelord

import java.io.File

/** The production source under `src/main/kotlin/forgelord/`, read by the structure tests. */
object Sources {
    val root = File("src/main/kotlin/forgelord")

    /**
     * The folders the architecture is told in: those under `forgelord/`, two levels down, except
     * the components inside `components/`. `quests`, `quests/client` and `quests/web` are three.
     */
    val folders: List<String> =
        root.walkTopDown().maxDepth(2)
            .filter { it.isDirectory && it != root }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filterNot { it.startsWith("components/") }
            .sorted()
            .toList()

    /** Every file with [extension] under [dir], a package path such as `components`. */
    fun files(
        dir: String,
        extension: String = "kt",
    ): List<File> =
        File(root, dir)
            .walkTopDown()
            .filter { it.isFile && it.extension == extension }
            .sortedBy { it.path }
            .toList()

    /** A line in one folder that uses another: the [folder] used, and [where] as `path:line: text`. */
    data class Use(
        val folder: String,
        val where: String,
    )

    /**
     * Every line in [folder] that uses another of the [folders], by import or by full name. A
     * folder's files are its own and not those of a folder below it: `quests` does not answer
     * for `quests/web`. The `package` line is left out, and so are comments and text in strings:
     * `"forgelord.ledger.url"` is the name of a setting, not a reference to code.
     */
    fun uses(folder: String): List<Use> =
        files(folder)
            .filter { folderOf(it.parentFile.relativeTo(root).invariantSeparatorsPath.split("/")) == folder }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val text = line.trim()
                    if (listOf("package ", "//", "/*", "*").any { text.startsWith(it) }) return@mapIndexedNotNull null
                    val code = text.replace(Regex(""""(?:\\.|[^"\\])*""""), "\"\"")
                    // The lower-case names after `forgelord.` are the package, and perhaps a function.
                    val used = Regex("""\bforgelord((?:\.[a-z]+\b)+)""").find(code)?.let { folderOf(it.groupValues[1].drop(1).split(".")) }
                    if (used != null && used != folder) Use(used, "${file.path}:${index + 1}: $text") else null
                }
            }

    /** The folder that [path] is in or under: the longest of [folders] that starts it. */
    private fun folderOf(path: List<String>): String? =
        path.indices.reversed()
            .map { path.take(it + 1).joinToString("/") }
            .firstOrNull { it in folders }
}
