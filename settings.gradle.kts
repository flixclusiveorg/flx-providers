pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "flx-provider") {
                useModule("com.github.flixclusiveorg.core-gradle:core-gradle:${requested.version}")
            }
        }
    }

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal() // <- For testing
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal() // <- For testing
    }
}

rootProject.name = "flx-providers"

// Allows `projects.util` instead of `project(":util")`
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

/**
*
* This file sets what projects are included.
* Every time you add a new project, you must add it
* to the includes below.
* */
include(
    "util",
    "Trakt",
    "Stremio",
    "TMDB",
)


/**
 * This is required because providers are in the ExampleProviders/kotlin subdirectory.
 *
 * Assuming you put all your providers into the project root, so on the same
 * level as this file, simply remove everything below.
 *
 * Otherwise, if you want a different structure, for example all
 * providers in a folder called "providers",
 * then simply change the path to `file("providers/${it.name})`
 */
rootProject.children.forEach {
    if (it.name == "util") {
        it.projectDir = file("core/util")
        return@forEach
    }

    it.projectDir = file("providers/${it.name}")
}
