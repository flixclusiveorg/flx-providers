import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

dependencies {
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

flixclusive {
    // ====== Provider Description =======
    description.set("My first provider!")

    /**
     *
     * Versions by level/hierarchy.
     * Increment one of these to trigger the updater
     * */
    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0
    // set custom versionName to override default version name

    // Changelog of your plugin
    changelog.set(
        """
        Some changelog:
        
        TODO: Add your changes here...
        """.trimIndent()
    ) // OPTIONAL

    /**
     * Image or Gif that will be shown at the top of your changelog page
     * */
    // changelogMedia.set("https://cool.png") // OPTIONAL

    /**
     * Add additional authors to this plugin
     * */
    author("SecondAuthor")
    // author( ... )

    /**
     * If your provider has an icon, put its image url here.
     * */
    // iconUrl.set("https://cool.png") // OPTIONAL

    /**
     * The main language of your provider.
     *
     * There are two supported values:
     * - Language.Multiple
     *      > Obviously for providers w/ multiple language support.
     * - Language("en")
     *      > For specific languages only. NOTE: Use the language's short-hand code.
     */
    language.set(Language.Multiple)

    /**
     * The main type that your provider supports.
     *
     * These are the possible values you could set:
     * - ProviderType.All
     * - ProviderType.TvShows
     * - ProviderType.Movies
     * - ProviderType(customType: String) // i.e., ProviderType("Anime")
     */
    providerType.set(ProviderType.All)

    /**
     * The current status of this provider.
     *
     * These are the possible values you could set:
     * - Status.Beta
     * - Status.Maintenance
     * - Status.Down
     * - Status.Working
     */
    status.set(Status.Working)
    // ================


    // === Utilities ===
    /**
     * Toggle this if you want to use the resources (R) of the main application.
     */
    // requiresResources.set(true) // OPTIONAL

    /**
     * Excludes this plugin from the updater,
     * meaning it won't show up for users.
     * Set this if the plugin is unfinished
     */
    // excludeFromUpdaterJson.set(true) // OPTIONAL
    // =================
}

