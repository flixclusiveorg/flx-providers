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
    description.set("""
        NOTICE: This provider uses WebView to scrape content. This might lag.
        
        Stream seamlessly in adjustable HD resolution (1080p) with blazing-fast loading speeds. Offers HLS media.
    """.trimIndent())

    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 2

    // Extra authors for specific provider
    // author(
    //    name = "...",
    //    githubLink = "https://github.com/...",
    // )
    // ===

    iconUrl.set("https://i.imgur.com/LNtqPTi.png")

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Maintenance)
}

