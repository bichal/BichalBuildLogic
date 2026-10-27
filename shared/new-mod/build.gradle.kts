import net.bichal.gradle.BichalModExtension

// Shared build logic: bichal-build-logic's bichal.neoforge-mod (dependencies,
// resources, legal files, natives, buildAndCollect). Only what is specific to
// {{name}} belongs here.
plugins {
    id("bichal.neoforge-mod")
    idea
}

val modProps = extensions.getByType(BichalModExtension::class.java).props
val mainSourceSet = sourceSets.main.get()

neoForge {
    runs {
        all {
            val runDir = rootProject.file("versions/${modProps.currentVersion}/run")
            if (!runDir.exists()) runDir.mkdirs()

            sourceSet = mainSourceSet
            gameDirectory = runDir
        }

        register("client") {
            client()
        }

        register("server") {
            server()
            programArgument("--nogui")
        }
    }
}
