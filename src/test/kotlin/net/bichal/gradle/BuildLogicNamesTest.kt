package net.bichal.gradle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class BuildLogicNamesTest {

    @ParameterizedTest
    @ValueSource(strings = [
        "bichal-build-logic",
        "BichalBuildLogic",
        "bichal_build_logic",
        "PanzerBuildLogic",
        "panzer-build-logic",
        "PANZER_BUILD_LOGIC",
        "Panzer.Build.Logic",
        "bichal-logic",
    ])
    fun `accepts the build-logic names in any case and separator`(name: String) {
        assertTrue(BuildLogicNames.matches(name), "$name should be recognized")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Celeris", "Velox", "build-logic-old", "BichalBuildLogicCopy", ""])
    fun `rejects other folders`(name: String) {
        assertFalse(BuildLogicNames.matches(name), "$name should not be recognized")
    }

    @Test
    fun `normalizes to letters and digits in lowercase`() {
        assertTrue(BuildLogicNames.normalize("Panzer-Build_Logic 2") == "panzerbuildlogic2")
    }

    @Test
    fun `locates the sibling checkout under an accepted name`(@TempDir root: File) {
        val mod = File(root, "Celeris").apply { mkdirs() }
        val logic = File(root, "PanzerBuildLogic").apply { mkdirs() }
        File(logic, "settings.gradle.kts").writeText("")
        File(root, "Velox").apply { mkdirs() }.resolve("settings.gradle.kts").writeText("")

        val found = BuildLogicNames.locate(mod)
        assertTrue(found?.name == "PanzerBuildLogic", "found $found")
    }

    @Test
    fun `a folder with an unaccepted name is not the build logic`(@TempDir root: File) {
        val mod = File(root, "Celeris").apply { mkdirs() }
        val other = File(root, "build-logic-old").apply { mkdirs() }
        File(other, "settings.gradle.kts").writeText("")

        assertTrue(BuildLogicNames.locate(mod) == null)
    }
}
