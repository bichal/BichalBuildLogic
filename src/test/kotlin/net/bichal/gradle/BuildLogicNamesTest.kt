package net.bichal.gradle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
}
