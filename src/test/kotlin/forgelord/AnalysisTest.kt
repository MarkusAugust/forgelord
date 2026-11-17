package forgelord

import io.github.markusaugust.streamlord.analysis.Analyzer
import io.github.markusaugust.streamlord.analysis.analyzeProject
import io.github.markusaugust.streamlord.analysis.failures
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The check the editors run as you type, over every source file. Write `"$quest.name"` where
 * `signal("quest.name")` belongs, and Kotlin takes the `$` for itself; this test fails the build.
 */
class AnalysisTest {
    @Test
    fun `no source file ships Datastar the browser would not understand`() {
        val failures = Analyzer().analyzeProject(File("src/main/kotlin")).failures()

        assertTrue(failures.isEmpty(), failures.joinToString("\n") { it.format() })
    }
}
