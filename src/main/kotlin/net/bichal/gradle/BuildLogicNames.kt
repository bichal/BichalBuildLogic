package net.bichal.gradle

import java.io.File

/**
 * Names a build-logic checkout may have, matched ignoring case and separators:
 * `bichal-build-logic`, `PanzerBuildLogic`, `panzer-build-logic`, `BICHAL_BUILD_LOGIC`...
 *
 * The shared settings script (shared/mod/settings.gradle.kts) has the same set, because it
 * has to find the checkout before this build is on the classpath. tools/test_layout.py
 * fails when the two lists drift apart.
 */
object BuildLogicNames {
    /** Normalized names: lowercase, letters and digits only. */
    val accepted: Set<String> = setOf("bichalbuildlogic", "panzerbuildlogic", "bichallogic", "panzerlogic")

    fun normalize(folderName: String): String = folderName.lowercase().filter { it.isLetterOrDigit() }

    fun matches(folderName: String): Boolean = normalize(folderName) in accepted

    /**
     * The build-logic checkout for a mod at [modRoot]: `BICHAL_BUILD_LOGIC_DIR` if it points at
     * one, otherwise the sibling folder with an accepted name. Null when there is none.
     */
    fun locate(modRoot: File): File? {
        val explicit = System.getenv("BICHAL_BUILD_LOGIC_DIR")?.let { File(it) }
        if (explicit != null && explicit.resolve("settings.gradle.kts").isFile) return explicit
        return modRoot.parentFile?.listFiles()?.firstOrNull {
            it.isDirectory && it.resolve("settings.gradle.kts").isFile && matches(it.name)
        }
    }
}
