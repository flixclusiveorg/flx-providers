rootProject.name = "flx-providers"

include(
    "SuperStream",
    "FlixHQ",
    "Sudo-Flix",
)

rootProject.children.forEach {
    it.projectDir = file("providers/${it.name}")
}
