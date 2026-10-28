package net.bichal.gradle

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `plugins { id("bichal.stonecutter") }` in a mod's stonecutter.gradle.kts:
 * applies the Stonecutter controller and the parameters every mod shares, so
 * that file only keeps the `stonecutter active "<version>"` line Stonecutter
 * itself rewrites when the active version changes.
 *
 * Swaps `mod_version` / `minecraft` (string literals for the version being
 * built) and constants `release` (false only for the "template" mod id),
 * `fabric` and `neoforge` (the node's loader: `<version>-fabric` nodes build
 * for Fabric), so loader-specific code reads `//? if fabric {`.
 *
 * `[stonecutter.replace.<name>]` tables (`since`, `from`, `to`) are string
 * replacements in the sources of every Minecraft version from `since` on: the
 * sources are written for the oldest version and renamed API is swapped in.
 */
class BichalStonecutterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("dev.kikugie.stonecutter")

        val tables = TomlBlockReader.parse(project.file("stonecutter.properties.toml"))
        fun mod(key: String): String = TomlBlockReader.find(tables, "mod")?.entries?.get(key)
            ?: error("[bichal.stonecutter] '$key' is missing under [mod] in stonecutter.properties.toml.")
        val modVersion = mod("version")
        val modId = mod("id")

        val replaceTables = tables.filter { it.path.size == 3 && it.path[0] == "stonecutter" && it.path[1] == "replace" }

        project.extensions.getByType(StonecutterControllerExtension::class.java).parameters {
            swaps.put("mod_version", "\"$modVersion\";")
            swaps.put("minecraft", "\"${node.metadata.version}\";")
            constants.put("release", modId != "template")
            val fabric = node.metadata.project.endsWith("-${BichalSettingsPlugin.FABRIC}")
            constants.put(BichalSettingsPlugin.FABRIC, fabric)
            constants.put(BichalSettingsPlugin.NEOFORGE, !fabric)
            replacements {
                for (table in replaceTables) {
                    val name = table.path[2]
                    val since = table.entries["since"] ?: error("[bichal.stonecutter] [stonecutter.replace.$name] needs 'since'.")
                    val from = table.entries["from"] ?: error("[bichal.stonecutter] [stonecutter.replace.$name] needs 'from'.")
                    val to = table.entries["to"] ?: error("[bichal.stonecutter] [stonecutter.replace.$name] needs 'to'.")
                    string(current.parsed >= since) { replace(from, to) }
                }
            }
        }
    }
}
