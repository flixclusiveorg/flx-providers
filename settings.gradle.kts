rootProject.name = "providers-template"

/**
*
* This file sets what projects are included.
* Every time you add a new project, you must add it
* to the includes below.
* */
include(
    "MyFirstProvider",
    // "MySecondProvider",
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
    it.projectDir = file("providers/${it.name}")
}
