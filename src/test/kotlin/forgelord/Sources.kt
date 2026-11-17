package forgelord

import java.io.File

/** The production source under `src/main/kotlin/forgelord/`, read by the structure tests. */
object Sources {
    val root = File("src/main/kotlin/forgelord")

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

    /**
     * Every line in [files] that refers to one of [packages], such as `quests`, by import or by
     * full name, as `path:line: text`. The `package` line itself is left out, and so is text in
     * strings: `"forgelord.ledger.url"` is the name of a setting, not a reference to code.
     */
    fun references(
        files: List<File>,
        packages: List<String>,
    ): List<String> =
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val text = line.trim()
                val code = text.replace(Regex(""""(?:\\.|[^"\\])*""""), "\"\"")
                val refers = packages.any { "forgelord.$it." in code || code.endsWith("forgelord.$it") }
                if (refers && !text.startsWith("package ")) "${file.path}:${index + 1}: $text" else null
            }
        }
}
