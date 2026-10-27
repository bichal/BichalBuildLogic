plugins {
    `kotlin-dsl`
    `maven-publish`
}

// group/version only matter for the published-plugin consumption path (see README
// "Two ways a mod can consume this repo"). When a mod uses the sibling-folder
// includeBuild path instead, Gradle substitutes the plugin directly from source and
// never looks at these coordinates.
group = "org.bichal.bichal"
version = providers.gradleProperty("bichalBuildLogicVersion").getOrElse("0.0.0-local")

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

// Versions come from gradle/libs.versions.toml (the `libs` accessors below), not from
// strings repeated here or parsed out of a TOML file by hand.
dependencies {
    implementation(libs.stonecutter.plugin)
    implementation(libs.moddev.plugin)
    implementation(libs.loom.plugin)
    implementation(libs.foojay.plugin)
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Class-based plugins. bichal.settings is a settings plugin: a mod's
// settings.gradle.kts applies it after includeBuild(<this checkout>),
// which puts this build (and Stonecutter, ModDevGradle, Foojay) on the settings
// classpath shared by every project of the mod.
gradlePlugin {
    plugins {
        register("bichalSettings") {
            id = "bichal.settings"
            implementationClass = "net.bichal.gradle.BichalSettingsPlugin"
        }
        register("bichalMod") {
            id = "bichal.mod"
            implementationClass = "net.bichal.gradle.BichalModPlugin"
        }
        register("bichalStonecutter") {
            id = "bichal.stonecutter"
            implementationClass = "net.bichal.gradle.BichalStonecutterPlugin"
        }
    }
}

// `kotlin-dsl` auto-registers every `src/main/kotlin/*.gradle.kts` precompiled script
// plugin (bichal.neoforge-mod, bichal.neoforge-mod-example) as a real Gradle plugin,
// markers included -- `maven-publish` just needs a `publishing {}` block to know where
// to put them. Publishing goes to a plain directory, not a real Maven server: CI copies
// that directory's contents onto the `gh-pages` branch (see
// .github/workflows/publish.yml) so consumers hit a static, unauthenticated URL.
publishing {
    repositories {
        maven {
            name = "GitHubPagesStaging"
            url = uri(layout.buildDirectory.dir("publish-repo"))
        }
    }
}
