import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

dependencies {
    implementation("androidx.core:core:1.13.1")
    /**
     * Custom dependencies for each provider should be implemented here.
     * */
    // implementation( ... )

    // Comment if not implementing own SettingsScreen
    val composeBom = platform("androidx.compose:compose-bom:2024.04.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    // ================= END: COMPOSE UI =================

}

android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

flxProvider {
    description.set("""
        A flixclusive adapter for Stremio addons. Torrent addons, such as Torrentio,  don't work without debrid accounts.
    """.trimIndent())

    changelog.set("""
        ### ✨ New:
        - Add search method to search for movies/series.
    """.trimIndent())

    versionMajor = 1
    versionMinor = 2
    versionPatch = 0
    versionBuild = 0

    // Extra authors for specific provider
    // author(
    //    name = "...",
    //    githubLink = "https://github.com/...",
    // )
    // ===

    iconUrl.set("https://i.imgur.com/Hoq93zL.png") // OPTIONAL

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Working)

    requiresResources.set(true)
}

