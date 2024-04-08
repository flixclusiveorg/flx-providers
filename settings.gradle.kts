rootProject.name = "flx-providers"

include(
    "SuperStream",
    "FlixHQ",
)

rootProject.children.forEach {
    it.projectDir = file("providers/${it.name}")
}
