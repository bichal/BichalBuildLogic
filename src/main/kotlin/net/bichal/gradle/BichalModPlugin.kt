package net.bichal.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `plugins { id("bichal.mod") }`: the build plugin of a mod built for more than
 * one loader. Each Stonecutter node applies the plugin of its loader:
 * bichal.fabric-mod on `<version>-fabric` nodes, bichal.neoforge-mod on the
 * others. Loader-specific build script code goes through
 * `pluginManager.withPlugin("net.neoforged.moddev") { ... }` (or Loom's id).
 */
class BichalModPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val fabric = project.name.endsWith("-${BichalSettingsPlugin.FABRIC}")
        project.pluginManager.apply(if (fabric) "bichal.fabric-mod" else "bichal.neoforge-mod")
    }
}
