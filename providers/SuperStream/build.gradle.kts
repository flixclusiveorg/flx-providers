import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import org.jetbrains.kotlin.konan.properties.Properties

dependencies {
    implementation("androidx.core:core:1.13.1")
    
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

    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "SUPERSTREAM_FIRST_API", "\"${properties.getProperty("SUPERSTREAM_FIRST_API")}\"")
        buildConfigField("String", "SUPERSTREAM_SECOND_API", "\"${properties.getProperty("SUPERSTREAM_SECOND_API")}\"")
        buildConfigField("String", "SUPERSTREAM_THIRD_API", "\"${properties.getProperty("SUPERSTREAM_THIRD_API")}\"")
        buildConfigField("String", "SUPERSTREAM_FOURTH_API", "\"${properties.getProperty("SUPERSTREAM_FOURTH_API")}\"")
    }
}

flxProvider {
    description.set("""
        REMINDER: This provider needs configuration to work!! Go to its provider settings and configure it there.

        REMINDER (again): This provider has cloudflare and will eventually prompt up WebView requests to solve CF challenges.
        
        A classic streaming service with a large library of movies and TV shows, some even in 4K. Majority of the content included on this provider offers non-HLS streaming.
    """.trimIndent())

    changelog.set("""
        # v1.4.2
        
        ### 🔧 Changes:
        - Fix CookieHelper error
        - Fix outdated webview user agents
    """.trimIndent())

    versionMajor = 1
    versionMinor = 5
    versionPatch = 0
    versionBuild = 1

    iconUrl.set("https://i.imgur.com/KgMakl9.png") // OPTIONAL

    language.set(Language.Multiple)

    providerType.set(ProviderType.All)

    status.set(Status.Working)

    requiresResources.set(true)
}

